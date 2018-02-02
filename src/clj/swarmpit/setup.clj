(ns swarmpit.setup
  (:require [clojure.tools.logging :as log]
            [swarmpit.config :as cfg]
            [swarmpit.docker.engine.client :as dc]))

(defn docker
  []
  (let [docker-version (dc/version)
        docker-api (str (:ApiVersion docker-version))
        docker-engine (:Version docker-version)]
    (swap! cfg/default assoc :docker-api docker-api)
    (swap! cfg/default assoc :docker-engine docker-engine)
    (log/info "Docker API:" (cfg/config :docker-api))
    (log/info "Docker ENGINE:" (cfg/config :docker-engine))
    (log/info "Docker SOCK:" (cfg/config :docker-sock))))
