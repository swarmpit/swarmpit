(ns swarmpit.api
  (:require [swarmpit.docker.client :as dc]
            [swarmpit.registry.client :as rc]
            [swarmpit.couchdb.client :as cc]
            [swarmpit.domain :as dom]
            [swarmpit.utils :refer [in?]]))

;;; Service API

(defn services
  []
  (->> (dc/get "/services")
       (dom/<-services)))

(defn services-name
  []
  (->> (services)
       (map (fn [s] [(:id s) (:serviceName s)]))
       (into (sorted-map))))

(defn service
  [service-id]
  (->> (str "/services/" service-id)
       (dc/get)
       (dom/<-service)))

(defn delete-service
  [service-id]
  (->> (str "/services/" service-id)
       (dc/delete)))

(defn create-service
  [service]
  (->> (dom/->service service)
       (dc/post "/services/create")))

(defn update-service
  [service-id service]
  (->> (dom/->service service)
       (dc/post (str "/services/" service-id "/update?version=" (:version service)))))

;;; Network API

(defn networks
  []
  (->> (dc/get "/networks")
       (dom/<-networks)))

(defn network
  [network-id]
  (->> (str "/networks/" network-id)
       (dc/get)
       (dom/<-network)))

(defn delete-network
  [network-id]
  (->> (str "/networks/" network-id)
       (dc/delete)))

(defn create-network
  [network]
  (->> (dom/->network network)
       (dc/post "/networks/create")))

;;; Node API

(defn nodes
  []
  (->> (dc/get "/nodes")
       (dom/<-nodes)))

(defn node
  [node-id]
  (->> (str "/nodes/" node-id)
       (dc/get)
       (dom/<-node)))

(defn nodes-name
  []
  (->> (nodes)
       (map (fn [n] [(:id n) (:name n)]))
       (into (sorted-map))))

;;; Task API

(defn tasks
  []
  (let [services (services-name)
        nodes (nodes-name)]
    (-> (dc/get "/tasks")
        (dom/<-tasks services nodes))))

(defn task
  [task-id]
  (let [services (services-name)
        nodes (nodes-name)]
    (-> (str "/tasks/" task-id)
        (dc/get)
        (dom/<-task services nodes))))

;;; Registry API

(defn registries
  []
  (->> (rc/headers "25a0e035-d147-4401-a4e8-5792ef0ec8fe" "feflryjn3olmvkdb")
       (rc/get "/_catalog")
       :repositories))

;;; User API

(defn registry
  [registry-id]
  (cc/load-record registry-id))

(defn create-registry
  [registry]
  (cc/save-record registry))