(ns swarmpit.utils
  #?(:clj
     (:import java.util.UUID)))

#?(:clj
   (defn generate-uuid
     "Generate uuid"
     []
     (str (UUID/randomUUID))))

(defn remove-el
  "Remove element in vector on given index"
  [vector index]
  (vec (concat
         (subvec vector 0 index)
         (subvec vector (inc index)))))