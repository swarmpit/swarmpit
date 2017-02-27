(ns swarmpit.api
  (:require [swarmpit.docker.client :as dc]
            [swarmpit.registry.client :as rc]))

(defn services
  []
  (dc/get "/services"))