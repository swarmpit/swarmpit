(ns swarmpit.docker.filter)

(defn by-id
  [coll coll-id]
  (first (filter #(= (:id %)
                     coll-id) coll)))