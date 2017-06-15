(ns swarmpit.storage
  (:refer-clojure :exclude [get remove])
  (:require [swarmpit.token :as token]))

(def storage (.-localStorage js/window))

(defn add
  "Add entry into browser's localStorage."
  [key val]
  (.setItem storage key val))

(defn get
  "Get value from browser's localStorage by given `key`"
  [key]
  (.getItem storage key))

(defn remove
  "Remove value from browser's localStorage by given `key`"
  [key]
  (.removeItem storage key))

(defn claims
  []
  (->> (get "token")
       (token/decode-jwt)))

(defn user
  []
  (get-in (claims) [:usr :username]))

(defn email
  []
  (get-in (claims) [:usr :email]))

(defn admin?
  []
  (= "admin"
     (get-in (claims) [:usr :role])))