(ns swarmpit.api
  (:require [swarmpit.docker.client :as dc]
            [swarmpit.registry.client :as rc]))

(defn services
  ([] (dc/get "/services"))
  ([id] (dc/get (str "/services/" id))))

(defn remove-service
  [id]
  (dc/delete (str "/services/" id)))