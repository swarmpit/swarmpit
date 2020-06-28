(ns swarmpit.database
  (:require [swarmpit.couchdb.client :as cc]
            [swarmpit.influxdb.client :as ic]
            [swarmpit.config :refer [config]]
            [swarmpit.couchdb.migration :refer [migrate]]
            [taoensso.timbre :refer [info error]]))

(defn- couch-ready?
  []
  (try (cc/version)
       (catch Exception _ false)))

(defn- couch-schema-ready?
  []
  (try
    (cc/database-exist?)
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
  (info "Waiting for" db-type "...")
  (loop [n sec]
    (if (running-fn)
      (info "..." db-type "connected in" (- sec n) "sec")
      (if (zero? n)
        (do
          (error "..." db-type "connection timeout")
          (throw (ex-info (str db-type " timeout") nil)))
        (do
          (Thread/sleep 1000)
          (recur (dec n)))))))

(defn- create-couch-database
  []
  (cc/create-database))

(defn- create-influx-database
  []
  (ic/create-database))

(defn- setup-influx-database
  []
  (ic/create-an-hour-rp)
  (ic/create-a-day-rp)
  (ic/task-cq)
  (ic/host-cq)
  (ic/service-cq)
  (ic/service-max-usage-cq))

(defn init-couch
  []
  (wait-for-db 100 couch-ready? "CouchDB")
  (if (couch-schema-ready?)
    (info "Swarmpit DB already exist")
    (do
      (create-couch-database)
      (info "Swarmpit DB successfully created")))
  (migrate))

(defn init-influx
  []
  (when (influx-configured?)
    (do
      (wait-for-db 100 influx-ready? "InfluxDB")
      (create-influx-database)
      (setup-influx-database)
      (info "InfluxDB RP:" (ic/retention-policy-summary))
      (info "InfluxDB CQ:" (ic/continuous-query-summary)))))

(defn init
  []
  (init-couch)
  (init-influx))