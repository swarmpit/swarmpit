(ns swarmpit.utils)

(defn remove-el
  "Remove element in `vector` on given `index`"
  [vector index]
  (vec (concat
         (subvec vector 0 index)
         (subvec vector (inc index)))))

(defn select-keys* [m paths]
  "Better select-keys supporting also nested maps. E.g. [[:name] [:node :nodeName]]"
  (->> paths
       (map (fn [p]
              [(last p) (get-in m p)]))
       (into {})))

(defn merge-data
  "Recursively merge delta to current json map structure."
  [data delta]
  (cond (and (map? data)
             (map? delta)) (merge-with merge-data data delta)
        :else delta))