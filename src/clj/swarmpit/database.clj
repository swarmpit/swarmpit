(ns swarmpit.database
  (:require [clojure.tools.logging :as log]
            [swarmpit.couchdb.client :as cc]
            [swarmpit.influxdb.client :as ic]
            [swarmpit.config :refer [config]]
            [swarmpit.couchdb.migration :refer [migrate]]))

(defn- couch-ready?
  []
  (try (cc/version)
       (catch Exception _ false)))

(defn- influx-ready?
  []
  (try (ic/ping)
       (catch Exception _ false)))

(defn- influx-configured?
  []
  (some? (config :influxdb-url)))

(defn- wait-for-db
  [sec running-fn db-type]
  (log/info (str "Waiting for " db-type " ..."))
  (loop [n sec]
    (if (running-fn)
      (log/info "... connected after" (- sec n) "sec")
      (if (zero? n)
        (do
          (log/error "... timeout")
          (throw (ex-info (str db-type " timeout") nil)))
        (do
          (Thread/sleep 1000)
          (recur (dec n)))))))

(defn- create-couch-database
  []
  (try
    (cc/create-database)
    (catch Exception ex
      (:body (ex-data ex)))))

(defn- create-influx-database
  []
  (ic/create-database))

(defn init-couch
  []
  (wait-for-db 100 couch-ready? "CouchDB")
  (when (not (:error (create-couch-database)))
    (log/info "DB schema created"))
  (migrate))

(defn init-influx
  []
  (when (influx-configured?)
    (do
      (wait-for-db 100 influx-ready? "InfluxDB")
      (create-influx-database))))

(defn init
  []
  (init-couch)
  (init-influx))