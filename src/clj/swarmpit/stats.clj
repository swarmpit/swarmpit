(ns swarmpit.stats
  (:require [clojure.core.cache :as cache]))

(def cache (atom (cache/basic-cache-factory {})))

(defn create
  [stats]
  (swap! cache assoc (:id stats) stats))

(defn stats
  [node-id]
  (get @cache node-id))
