(ns swarmpit.api
  (:require [swarmpit.docker.client :as dc]
            [swarmpit.registry.client :as rc]
            [swarmpit.domain :as dom]
            [swarmpit.utils :refer [in?]]))

;;; Service API

(defn services
  []
  (->> (dc/get "/services")
       (dom/<-services)))

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

;;; Network API

(defn networks
  []
  (->> (dc/get "/networks")
       (dom/<-networks)
       (filter #(not (in? ["host" "null" "bridge"] (:driver %))))))

(defn network
  [network-id]
  (->> (str "/networks/" network-id)
       (dc/get)
       (dom/<-network)))