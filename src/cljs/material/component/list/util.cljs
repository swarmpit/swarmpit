(ns material.component.list.util
  (:refer-clojure :exclude [filter])
  (:require [swarmpit.utils :refer [map-values]]))

(defn- primary-key
  [render-metadata]
  (->> (clojure.core/filter #(= true (:primary %)) render-metadata)
       (first)
       :key))

(defn render-keys
  [render-metadata]
  (->> render-metadata
       (map :key)
       (into [])))

(defn render-value?
  [value]
  (if value
    (if (coll? value)
      (not (empty? value))
      true)
    false))

(defn filter
  [items query]
  (if (or (empty? query)
          (< (count query) 2))
    items
    (clojure.core/filter
      (fn [item]
        (->> (map-values item)
             (clojure.core/filter #(clojure.string/includes? % query))
             (empty?)
             (not))) items)))
