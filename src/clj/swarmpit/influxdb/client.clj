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

(defn read-task-stats-l [task-name]
  (read-doc
    (str "SELECT cpu, memory FROM swarmpit..task_stats WHERE task = '" task-name "' AND time > now() - 7d")))

(defn read-task-stats [task-name]
  (read-doc
    (str "SELECT MAX(cpu) as cpu, MAX(memory) as memory FROM swarmpit..task_stats WHERE task = '" task-name "' AND time > now() - 1d GROUP BY time(1m)")))

(defn read-service-atats [service-name nodes-count]
  (read-doc
    (str "SELECT SUM(cpu) / " nodes-count " as cpu, SUM(memory) as memory FROM swarmpit..task_stats WHERE service = '" service-name "' AND time > now() - 1d GROUP BY time(30s)")))