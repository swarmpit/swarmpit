(ns swarmpit.storage
  (:refer-clojure :exclude [get remove]))

(def storage (.-localStorage js/window))

(defn add
  "Add entry into browser's localStorage."
  [key val]
  (.setItem storage key val))

(defn get
  "Get value from browser's localStorage by given `key'"
  [key]
  (.getItem storage key))

(defn remove
  "Remove value from browser's localStorage by given `key`"
  [key]
  (.removeItem storage key))