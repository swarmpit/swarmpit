(ns swarmpit.setup
  (:require [swarmpit.log :as log]
            [swarmpit.config :as cfg]
            [swarmpit.docker.engine.client :as dc]
            [taoensso.timbre :as timbre :refer [info warn]]))

(def ^:private supported-min "1.30")
(def ^:private supported-max "1.44")

(defn- parse-api
  [s]
  (when s
    (try (Double/parseDouble s) (catch Exception _ nil))))

(defn- negotiate-api
  "Pick an API version that both swarmpit and the daemon understand.
   daemon-max / daemon-min come from /_ping headers."
  [daemon-max daemon-min]
  (let [our-min (parse-api supported-min)
        our-max (parse-api supported-max)
        dmax (or (parse-api daemon-max) our-max)
        dmin (or (parse-api daemon-min) our-min)
        top (min our-max dmax)
        floor (max our-min dmin)
        chosen (max floor top)]
    (String/format java.util.Locale/ROOT "%.2f" (into-array Object [chosen]))))

(defn docker
  []
  (let [env-override (:swarmpit-docker-api cfg/environment)]
    (try
      (let [ping (dc/ping)
            headers (:headers ping)
            daemon-max (or (:api-version headers) (:Api-Version headers))
            daemon-min (or (:api-minimum-version headers) (:Api-Minimum-Version headers))]
        (when-not env-override
          (let [negotiated (negotiate-api daemon-max daemon-min)]
            (swap! cfg/default assoc :docker-api negotiated)
            (info "Docker API negotiated:" negotiated
                  (str "(daemon " daemon-min "–" daemon-max
                       ", swarmpit " supported-min "–" supported-max ")")))))
      (catch Exception e
        (warn "Docker API autodetect failed, keeping configured value:" (.getMessage e))))
    (try
      (let [docker-version (dc/version)
            docker-engine (:Version docker-version)]
        (swap! cfg/default assoc :docker-engine docker-engine))
      (catch Exception e
        (warn "Docker /version lookup failed:" (.getMessage e))))
    (when env-override
      (info "Docker API override from env:" env-override))
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