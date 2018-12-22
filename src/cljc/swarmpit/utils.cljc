(ns swarmpit.utils
  (:require [clojure.walk]
            #?(:clj [flatland.ordered.map :refer [ordered-map]])))

(defn remove-el
  "Remove element in `vector` on given `index`"
  [vector index]
  (vec (concat
         (subvec vector 0 index)
         (subvec vector (inc index)))))

(defn select-keys*
  "Better select-keys supporting also nested maps. E.g. [[:name] [:node :nodeName]]"
  [m paths]
  (->> paths
       (map (fn [p]
              [(last p) (get-in m p)]))
       (into {})))

(defn merge-data
  "Recursively merge map delta to current data structure."
  [m delta]
  (cond (and (map? m)
             (map? delta)) (merge-with merge-data m delta)
        :else delta))

(defn map-values
  "Walk the map and return all values as set"
  [m]
  (remove nil?
          (set
            (flatten
              (clojure.walk/prewalk
                (fn [x]
                  (cond
                    (map? x) (vals x)
                    (number? x) (str x)
                    (boolean? x) (str x)
                    :else (identity x))) m)))))

(defn empty-or-nil?
  [val]
  (if (coll? val)
    (empty? val)
    (nil? val)))

(defn clean
  "remove pairs of key-value that are nil or empty from a (possibly nested) map."
  [map]
  (clojure.walk/postwalk
    #(if (map? %)
       (let [nm #?(:clj  (if (instance? flatland.ordered.map.OrderedMap %)
                           (ordered-map)
                           {})
                   :cljs {})
             m (into nm (remove (comp empty-or-nil? val) %))]
         (when (seq m) m))
       %)
    map))

(defn name-value->map
  [name-value-coll]
  (->> name-value-coll
       (map #(hash-map (keyword (:name %)) (:value %)))
       (into {})))

(defn map->name-value
  [map-col]
  (->> map-col
       (map (fn [l] {:name  (name (key l))
                     :value (val l)}))
       (into [])))

(defn ->nano
  [number]
  (when number (* number 1000000000)))

(defn nano->
  [number]
  (when number (/ number 1000000000)))
