(ns swarmpit.setup
  (:require [swarmpit.log :as log]
            [swarmpit.config :as cfg]
            [swarmpit.docker.engine.client :as dc]
            [taoensso.timbre :as timbre :refer [info]]))

(defn docker
  []
  (let [docker-version (dc/version)
        docker-api (str (:ApiVersion docker-version))
        docker-engine (:Version docker-version)]
    (swap! cfg/default assoc :docker-api docker-api)
    (swap! cfg/default assoc :docker-engine docker-engine)
    (info "Docker API:" (cfg/config :docker-api))
    (info "Docker ENGINE:" (cfg/config :docker-engine))
    (info "Docker SOCK:" (cfg/config :docker-sock))))

(defn log
  []
  (let [log-level (cfg/config :log-level)]
    (info "Log level:" log-level)
    (timbre/merge-config!
      {:level     (keyword log-level)
       :output-fn (fn [data] (log/output-fn data))})))