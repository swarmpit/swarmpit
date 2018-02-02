(ns swarmpit.docker.engine.log
  (:require [clojure.string :refer [split-lines trim split join]]))

(defn- parse-log-line
  [line]
  (->> (split line #" ")
       (split-at 2)
       (second)
       (join " ")))

(defn- parse-log-timestamp
  [line]
  (-> (re-pattern "(19|20)\\d{2}[.:\\-TZ0-9]* ")
      (re-find line)
      (first)
      (or "")
      (trim)))

(defn- parse-log-task
  [line]
  (-> (str "com.docker.swarm.task.id=([a-z0-9]+)")
      (re-pattern)
      (re-find line)
      (second)))

(defn parse-log
  [service-log]
  (->> (split-lines service-log)
       (map (fn [x] {:line      (parse-log-line x)
                     :timestamp (parse-log-timestamp x)
                     :task      (parse-log-task x)}))
       (filter #(some? (:task %)))
       (sort-by :timestamp)))