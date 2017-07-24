(ns swarmpit.config
  (:require
    [environ.core :refer [env]]))

(def default {:docker-sock "/var/run/docker.sock"
              :docker-api  "v1.28"
              :db-url      "http://localhost:5984"})

(defn environment
  [] (->> {:docker-sock (env :swarmpit-docker-sock)
           :docker-api  (env :swarmpit-docker-api)
           :db-url      (env :swarmpit-db)}
          (into {} (remove #(nil? (val %))))))

(def ^:private dynamic (atom {}))

(defn update!
  [config] (reset! dynamic config))

(defn config
  ([] (->> [default (environment) @dynamic]
          (apply merge)))
  ([key] ((config) key)))