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
  (try
    (cc/create-database)
    (catch Exception ex
      (get-in (ex-data ex) [:body :error]))))

;;; User API

(defn users
  []
  (cc/users))

(defn user
  [user-id]
  (cc/user user-id))

(defn user-by-credentials
  [credentails]
  (cc/user-by-credentials (:username credentails)
                          (cmo/->password (:password credentails))))

(defn user-by-username
  [username]
  (cc/user-by-username username))

(defn user-exist?
  [user]
  (some? (user-by-username (:username user))))

(defn create-user
  [user]
  (if (not (user-exist? user))
    (->> (cmo/->user user)
         (cc/create-user))))

;;; Registry API

(defn registries
  []
  (cc/registries))

(defn registries-sum
  []
  (->> (registries)
       (map #(select-keys % [:name :version]))))

(defn registry
  [registry-id]
  (cc/registry registry-id))

(defn registry-by-name
  [registry-name]
  (cc/registry-by-name registry-name))

(defn registry-exist?
  [registry]
  (some? (registry-by-name (:name registry))))

(defn registry-valid?
  [registry]
  (some?
    (try
      (case (:version registry)
        "v1" (rc/v1-info registry)
        "v2" (rc/v2-info registry))
      (catch Exception _))))

(defn create-registry
  [registry]
  (if (not (registry-exist? registry))
    (->> (cmo/->registry registry)
         (cc/create-registry))))

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
  (let [registry-name (get-in service [:repository :registry])
        registry (registry-by-name registry-name)
        auth-config (dmo/->auth-config registry)]
    (->> (dmo/->service-image-create service registry)
         (dmo/->service service)
         (dc/create-service auth-config))))

(defn update-service
  [service-id service]
  (let [service-version (:version service)]
    (->> (dmo/->service-image-update service)
         (dmo/->service service)
         (dc/update-service service-id service-version))))

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

;;; Repository API

(defn dockerhub-repositories
  [registry-name repository-query repository-page]
  (let [registry (registry-by-name registry-name)]
    (-> (rc/dockerhub-repositories registry repository-query repository-page)
        (rci/->dockerhub-repositories repository-query repository-page))))

(defn v1-repositories
  [registry-name repository-query repository-page]
  (let [registry (registry-by-name registry-name)]
    (->> (rc/v1-repositories registry repository-query repository-page)
         (rci/->v1-repositories))))

(defn v2-repositories
  [registry-name repository-query]
  (let [registry (registry-by-name registry-name)]
    (->> (rc/v2-repositories registry)
         (filter #(string/includes? % (or repository-query "")))
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