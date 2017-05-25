(ns swarmpit.api
  (:require [digest :as d]
            [swarmpit.docker.client :as dc]
            [swarmpit.docker.mapper.inbound :as dmi]
            [swarmpit.docker.mapper.outbound :as dmo]
            [swarmpit.registry.client :as rc]
            [swarmpit.couchdb.client :as cc]))

;;; Database API

(defn create-database
  []
  (cc/put "/swarmpit"))

;;; User API

(defn create-user
  [user]
  (let [passwd (d/digest "sha-256" (:password user))]
    (->> (assoc user :password passwd
                     :type "user"
                     :role "admin")
         (cc/post "/swarmpit"))))

(defn user-by-email
  [email]
  (->> {:selector {:type  {"$eq" "user"}
                   :email {"$eq" email}}}
       (cc/post "/swarmpit/_find")
       :docs
       (first)))

(defn user-by-credentials
  [credentails]
  (->> {:selector {:type     {"$eq" "user"}
                   :email    {"$eq" (:user credentails)}
                   :password {"$eq" (d/digest "sha-256" (:password credentails))}}}
       (cc/post "/swarmpit/_find")
       :docs
       (first)))

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
  (->> (rc/headers "25a0e035-d147-4401-a4e8-5792ef0ec8fe" "feflryjn3olmvkdb")
       (rc/get "/_catalog")
       :repositories))

;;; Repository API

(defn repositories
  []
  (->> {:selector {:type {"$eq" "repository"}}}
       (cc/post "/swarmpit/_find")
       :docs))

(defn create-repository
  [repository]
  ())