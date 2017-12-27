(ns swarmpit.utils)

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
                    (integer? x) (str x)
                    (boolean? x) (str x)
                    :else (identity x))) m)))))