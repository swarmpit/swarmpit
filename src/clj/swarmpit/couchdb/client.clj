(ns swarmpit.couchdb.client
  (:require [com.ashafa.clutch :as clutch]))

(defn open-connection []
  (let [conn (doto
               (clutch/couch "swarmpit")
               (clutch/create!))]
    conn))

(def db-connection (delay (open-connection)))

(defn db [] @db-connection)

(defn load-record
  [id]
  {:body (get-in (db) [id])})

(defn save-record
  [data]
  (let [id (get-in data ["_id"])]
    (clutch/assoc! (db) id data)
    {:body (get-in (db) [id])}))