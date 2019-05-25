(ns swarmpit.docker.engine.log
  (:require [clojure.string :refer [blank? trim split split-lines join]]))

(defn parse-log-line
  [line]
  (->> (split line #" ")
       (split-at 2)
       (second)
       (join " ")))

(defn parse-log-timestamp
  [line]
  (-> (re-pattern "(19|20)\\d{2}[.:\\-TZ0-9]* ")
      (re-find line)
      (first)
      (or "")
      (trim)))

(defn- parse-service-task
  [line]
  (-> (str "com.docker.swarm.task.id=([a-z0-9]+)")
      (re-pattern)
      (re-find line)
      (second)))

(defn parse-service-log
  [service-log]
  (when (some? service-log)
    (->> (split-lines service-log)
         (map (fn [x] {:line      (parse-log-line x)
                       :timestamp (parse-log-timestamp x)
                       :task      (parse-service-task x)}))
         (filter #(some? (:task %)))
         (sort-by :timestamp))))

(defn parse-agent-task
  [line]
  (-> (split line #" ")
      (first)))

(defn parse-agent-log
  [service-log]
  (->> service-log
       (filter #(some? %))
       (map (fn [x]
              {:line      (parse-log-line x)
               :timestamp (parse-log-timestamp x)
               :task      (parse-agent-task x)}))
       (sort-by :timestamp)))

(defn format-log
  [task-log task-id]
  (if (blank? task-log)
    []
    (map #(str task-id " " %) (split-lines task-log))))