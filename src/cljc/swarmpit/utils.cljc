(ns swarmpit.utils
  #?(:clj
     (:import java.util.UUID)))

#?(:clj
   (defn generate-uuid
     "Generate uuid"
     []
     (str (UUID/randomUUID))))

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