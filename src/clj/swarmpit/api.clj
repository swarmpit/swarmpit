(ns swarmpit.api
  (:require [swarmpit.docker.client :as dc]
            [swarmpit.registry.client :as rc]
            ;[swarmpit.domain :refer [Service Port]]
            ))

(defn services
  ([] (dc/get "/services"))
  ([id] (dc/get (str "/services/" id))))