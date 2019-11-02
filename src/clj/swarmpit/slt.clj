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


