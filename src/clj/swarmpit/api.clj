(ns swarmpit.api
  (:require [swarmpit.docker.client :as dc]
            [swarmpit.registry.client :as rc]
            [swarmpit.domain :as dom]))

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
  (dc/delete (str "/services/" service-id)))

(defn create-service
  [service]
  (dc/post "/services/create" (dom/->service service)))