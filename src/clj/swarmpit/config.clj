(ns swarmpit.config
  (:require [environ.core :refer [env]]))

(def default
  (atom {:docker-sock      "/var/run/docker.sock"
         :docker-api       "1.30"
         :db-url           "http://localhost:5984"
         :work-dir         "/tmp"
         :password-hashing {:alg        :pbkdf2+sha512
                            :iterations 200000}}))

(def environment
  (->> {:docker-sock (env :swarmpit-docker-sock)
        :docker-api  (env :swarmpit-docker-api)
        :db-url      (env :swarmpit-db)
        :work-dir    (env :swarmpit-workdir)}
       (into {} (remove #(nil? (val %))))))

(def ^:private dynamic (atom {}))

(defn update!
  [config] (reset! dynamic config))

(defn config
  ([] (->> [@default environment @dynamic]
           (apply merge)))
  ([key] ((config) key)))