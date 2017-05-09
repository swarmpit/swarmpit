(ns swarmpit.utils)

(defn uuid
  "Generate uuid"
  []
  (str (java.util.UUID/randomUUID)))

(defn remove-el
  "Remove element in vector on given index"
  [vector index]
  (vec (concat
         (subvec vector 0 index)
         (subvec vector (inc index)))))

(defn in?
  "True if collection contains element"
  [coll elm]
  (some #(= elm %) coll))

(defn add-item
  "Add item to atom vector"
  [atom item]
  (swap! atom
         (fn [vec] (conj vec item))))

(defn remove-item
  "Remove item on given index of atom vector"
  [atom index]
  (swap! atom
         (fn [vec] (remove-el vec index))))

(defn update-item
  "Update map item on given index of atom vector"
  [atom index k v]
  (swap! atom
         (fn [vec] (assoc-in vec [index k] v))))