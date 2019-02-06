(ns swarmpit.storage
  (:refer-clojure :exclude [get remove])
  (:require [swarmpit.token :as token]
            [cognitect.transit :as t]))

(def storage (.-localStorage js/window))

(def r (t/reader :json))

(defn add
  "Add entry into browser's localStorage."
  [key val]
  (.setItem storage key val))

(defn add-map
  "Add map entry into browser's localStorage."
  [key map]
  (.setItem storage key (.stringify js/JSON (clj->js map))))

(defn get
  "Get value from browser's localStorage by given `key`"
  [key]
  (.getItem storage key))

(defn get-map
  "Get map from browser's localStorage by given `key`"
  [key]
  (t/read r (get key)))

(defn remove
  "Remove value from browser's localStorage by given `key`"
  [key]
  (.removeItem storage key))

(defn claims
  []
  (try
    (token/decode-jwt (get "token"))
    (catch js/Error _ nil)))

(defn user
  []
  (get-in (claims) [:usr :username]))

(defn email
  []
  (get-in (claims) [:usr :email]))

(defn admin?
  []
  (token/admin? (get-in (claims) [:usr])))
