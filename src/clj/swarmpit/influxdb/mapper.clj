(ns swarmpit.influxdb.mapper
  (:require [clojure.string :as str]
            [clojure.edn :as edn]))

(defn ->task-tags [task-name host-name]
  (let [segments (drop 1 (str/split task-name #"/|\."))]
    (if (= 1 segments)
      nil
      {:task    (str (first segments) "." (second segments))
       :service (first segments)
       :host    host-name})))

(defn round [value]
  (->> value
       (double)
       (format "%.2f")
       (edn/read-string)))

(defn ->host-tags [host-name]
  {:host host-name})

(defn ->memory-mb [bytes]
  (-> (/ bytes (* 1000 1000))
      (round)))

(defn ->cpu-round [percentage]
  (-> percentage
      (round)))