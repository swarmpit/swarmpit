(ns swarmpit.influxdb.client
  (:require [influxdb.client :as client]
            [influxdb.convert :as convert]
            [swarmpit.config :refer [config]]
            [clojure.tools.logging :as log]))

(defn- execute [fn]
  (let [conn {:url (config :influxdb-url)}]
    (fn conn)))

(defn- read-doc [q]
  (execute
    (fn [conn] (client/unwrap (client/query conn ::client/read q)))))

(defn- manage-doc [q]
  (execute
    (fn [conn] (client/unwrap (client/query conn ::client/manage q)))))

(defn- write-doc [point]
  (execute
    (fn [conn] (client/write conn "swarmpit" (convert/point->line point)))))

(defn create-database []
  (manage-doc "CREATE DATABASE swarmpit"))

(defn drop-database []
  (manage-doc "DROP DATABASE swarmpit"))

(defn list-databases []
  (read-doc "SHOW DATABASES"))

(defn write-task-points [tags cpu memory]
  (write-doc {:meas   "task_stats"
              :tags   tags
              :fields {:cpu    cpu
                       :memory memory}}))

(defn write-host-points [tags cpu memory]
  (write-doc {:meas   "host_stats"
              :tags   tags
              :fields {:cpu    cpu
                       :memory memory}}))

(defn read-task-stats [task-name]
  (read-doc
    (str "SELECT cpu, memory FROM swarmpit..task_stats WHERE task = '" task-name "'")))

;(defn read-service-cpu [service-name]
;  (read-doc
;    (str "SELECT SUM(value) as value FROM swarmpit..task_cpu WHERE service = '" service-name "' GROUP BY time(1m)")))