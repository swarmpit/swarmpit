(ns swarmpit.slt
  (:require [clojure.core.cache :as cache]
            [swarmpit.uuid :refer [uuid]]
            [swarmpit.base64 :as base64]))

(def cache (atom (cache/ttl-cache-factory {} :ttl 10000)))

(defn- generate
  []
  (->> (uuid)
       (base64/encode)))

(defn create
  [username]
  (let [slt (generate)]
    (swap! cache assoc slt username)
    slt))

(defn valid?
  [slt]
  (if (cache/has? @cache slt)
    true
    false))

(defn user
  [slt]
  (cache/lookup @cache slt))

(defn consume!
  "Validate the SLT and invalidate it. Returns the associated user on
   success, nil if the token is unknown/expired/already consumed."
  [slt]
  (when (and slt (cache/has? @cache slt))
    (let [u (cache/lookup @cache slt)]
      (swap! cache cache/evict slt)
      u)))


