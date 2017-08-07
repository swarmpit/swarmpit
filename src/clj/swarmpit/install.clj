(ns swarmpit.install
  (:require [swarmpit.couchdb.client :as cc]
            [swarmpit.couchdb.migration :refer [migrate]]
            [swarmpit.uuid :as uuid]))

(defn- create-database
  []
  (try
    (cc/create-database)
    (catch Exception ex
      (get-in (ex-data ex) [:body]))))

(defn init
  []
  (cc/db-version)
  (when (not (:error (create-database)))
    (println "DB schema created"))
  (migrate))