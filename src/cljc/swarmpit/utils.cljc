(ns swarmpit.utils)

(defn remove-el
  "Remove elem in vector in given index"
  [coll index]
  (vec (concat
         (subvec coll 0 index)
         (subvec coll (inc index)))))

