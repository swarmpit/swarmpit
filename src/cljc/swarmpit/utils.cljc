(ns swarmpit.utils)

(defn remove-el
  "Remove elem in vector on given index"
  [coll index]
  (vec (concat
         (subvec coll 0 index)
         (subvec coll (inc index)))))

(defn in?
  "True if coll contains elem"
  [coll elm]
  (some #(= elm %) coll))