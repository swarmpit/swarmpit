(ns swarmpit.api
  (:require [digest :as d]
            [clojure.core.memoize :as memo]
            [swarmpit.utils :as u]
            [swarmpit.docker.client :as dc]
            [swarmpit.docker.mapper.inbound :as dmi]
            [swarmpit.docker.mapper.outbound :as dmo]
            [swarmpit.registry.client :as rc]
            [swarmpit.couchdb.client :as cc]))

(defn create-database
  []
  (cc/put "/swarmpit"))

;;; User API

(defn users
  []
  (->> {:selector {:type {"$eq" "user"}}}
       (cc/post "/swarmpit/_find")
       :docs))

(defn user-by-credentials
  [credentails]
  (->> {:selector {:type     {"$eq" "user"}
                   :email    {"$eq" (:user credentails)}
                   :password {"$eq" (d/digest "sha-256" (:password credentails))}}}
       (cc/post "/swarmpit/_find")
       :docs
       (first)))

(defn create-user
  [user]
  (let [passwd (d/digest "sha-256" (:password user))]
    (->> (assoc user :password passwd
                     :type "user")
         (cc/post "/swarmpit"))))

;;; Service API

(defn services
  []
  (dmi/->services (dc/get "/services")
                  (dc/get "/tasks")
                  (dc/get "/nodes")))

(defn service
  [service-id]
  (dmi/->service (-> (str "/services/" service-id)
                     (dc/get))
                 (dc/get "/tasks")
                 (dc/get "/nodes")))

(defn delete-service
  [service-id]
  (-> (str "/services/" service-id)
      (dc/delete)))

(defn create-service
  [service]
  (->> (dmo/->service service)
       (dc/post "/services/create")))

(defn update-service
  [service-id service]
  (->> (dmo/->service service)
       (dc/post (str "/services/" service-id "/update?version=" (:version service)))))

;;; Network API

(defn networks
  []
  (-> (dc/get "/networks")
      (dmi/->networks)))

(defn network
  [network-id]
  (-> (str "/networks/" network-id)
      (dc/get)
      (dmi/->network)))

(defn delete-network
  [network-id]
  (-> (str "/networks/" network-id)
      (dc/delete)))

(defn create-network
  [network]
  (->> (dmo/->network network)
       (dc/post "/networks/create")))

;;; Node API

(defn nodes
  []
  (-> (dc/get "/nodes")
      (dmi/->nodes)))

(defn node
  [node-id]
  (-> (str "/nodes/" node-id)
      (dc/get)
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
  (->> {:selector {:type {"$eq" "registry"}}}
       (cc/post "/swarmpit/_find")
       :docs))

(defn create-registry
  [registry]
  (->> (assoc registry :type "registry")
       (cc/post "/swarmpit")))

;;; Repository API

(defn repositories-by-registry
  [registry]
  (->> (rc/headers (:user registry) (:password registry))
       (rc/get (:scheme registry) (:url registry) "/_catalog")
       :repositories))

(defn repositories
  []
  (->> (registries)
       (map #(->> (repositories-by-registry %)
                  (map (fn [repo] (into {:id          (hash (str repo (:url %)))
                                         :name        repo
                                         :registry    (:name %)
                                         :registryUrl (:url %)})))))
       (flatten)))

(def cached-repositories (memo/memo repositories))