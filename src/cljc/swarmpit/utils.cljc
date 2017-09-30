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

(defn parse-int
  [value]
  "Return value if integer representation otherwise nil"
  (when (integer? value)
    value))

(defn parse-float
  [value]
  "Return value if float representation otherwise nil"
  (when (float? value)
    value))