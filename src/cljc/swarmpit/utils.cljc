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
  (let [parsed (js/parseInt value)]
    (when (not (js/isNaN parsed))
      parsed)))

(defn parse-float
  [value]
  "Return value if float representation otherwise nil"
  (let [parsed (js/parseFloat value)]
    (when (not (js/isNaN parsed))
      parsed)))