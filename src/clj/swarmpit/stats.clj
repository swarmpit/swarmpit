(ns swarmpit.stats
  (:require [clojure.core.cache :as cache]))

(def cache (atom (cache/basic-cache-factory {})))

(defn create
  [stats]
  (swap! cache assoc (:id stats) stats))

(defn node-stats
  [node-id]
  (get @cache node-id))

(defn task-stats
  [task]
  (->> (node-stats (:nodeId task))
       :tasks
       (filter #(= (str "/" (:taskName task) "." (:id task))
                   (:name %)))
       (first)))