(ns swarmpit.api
  (:require [clojure.core.memoize :as memo]
            [clojure.string :as string]
            [swarmpit.utils :as u]
            [swarmpit.docker.client :as dc]
            [swarmpit.docker.mapper.inbound :as dmi]
            [swarmpit.docker.mapper.outbound :as dmo]
            [swarmpit.registry.client :as rc]
            [swarmpit.registry.mapper.inbound :as rci]
            [swarmpit.couchdb.client :as cc]
            [swarmpit.couchdb.mapper.outbound :as cmo]))

(defn create-database
  []
  (cc/create-database))

;;; User API

(defn users
  []
  (cc/users))

(defn user-by-credentials
  [credentails]
  (cc/user-by-credentials (:user credentails)
                          (cmo/->password (:password credentails))))

(defn create-user
  [user]
  (->> (cmo/->user user)
       (cc/create-user)))

;;; Service API

(defn services
  []
  (dmi/->services (dc/services)
                  (dc/tasks)
                  (dc/nodes)))

(defn service
  [service-id]
  (dmi/->service (dc/service service-id)
                 (dc/tasks)
                 (dc/nodes)))

(defn delete-service
  [service-id]
  (dc/delete-service service-id))

(defn create-service
  [service]
  (->> (dmo/->service service)
       (dc/create-service)))

(defn update-service
  [service-id service]
  (->> (dmo/->service service)
       (dc/update-service service-id)))

;;; Network API

(defn networks
  []
  (-> (dc/networks)
      (dmi/->networks)))

(defn network
  [network-id]
  (-> (dc/network network-id)
      (dmi/->network)))

(defn delete-network
  [network-id]
  (dc/delete-network network-id))

(defn create-network
  [network]
  (->> (dmo/->network network)
       (dc/create-network)))

;;; Node API

(defn nodes
  []
  (-> (dc/nodes)
      (dmi/->nodes)))

(defn node
  [node-id]
  (-> (dc/node node-id)
      (dmi/->node)))

;;; Task API

(defn tasks
  []
  (->> (services)
       (map #(:tasks %))
       (flatten)))

(defn task
  [task-id]
  (->> (tasks)
       (filter #(= (:id %) task-id))
       (first)))

;;; Registry API

(defn registries
  []
  (cc/registries))

(defn registries-sum
  []
  (->> (registries)
       (map #(select-keys % [:name :version]))))

(defn registry-by-name
  [registry-name]
  (cc/registry-by-name registry-name))

(defn create-registry
  [registry]
  (->> (cmo/->registry registry)
       (cc/create-registry)))

(defn v1-registry?
  [registry]
  (= "v1" (:version registry)))

(defn valid-registry?
  [registry]
  (try
    (some? (if (v1-registry? registry)
             (rc/v1-info registry)
             (rc/v2-info registry)))
    (catch Exception _
      false)))

;;; Repository API

(defn v1-repositories
  [registry-name repository-query repository-page]
  (let [registry (registry-by-name registry-name)]
    (->> (rc/v1-repositories registry repository-query repository-page)
         (rci/->v1-repositories))))

(defn v2-repositories
  [registry-name repository-query]
  (let [registry (registry-by-name registry-name)]
    (->> (rc/v2-repositories registry)
         (filter #(string/includes? % repository-query))
         (rci/->v2-repositories))))

(defn v1-tags
  [registry-name repository-name]
  (let [registry (registry-by-name registry-name)]
    (->> (rc/v1-tags registry repository-name)
         (map :name)
         (into []))))

(defn v2-tags
  [registry-name repository-name]
  (let [registry (registry-by-name registry-name)]
    (->> (rc/v2-tags registry repository-name)
         :tags)))