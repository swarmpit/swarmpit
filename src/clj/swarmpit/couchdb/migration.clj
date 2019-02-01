(ns swarmpit.couchdb.migration
  (:require [swarmpit.couchdb.client :as db]
            [swarmpit.api :as api]
            [swarmpit.uuid :refer [uuid]]))

(defn- create-secret
  []
  (db/create-secret {:secret (uuid)})
  (println "Default token secret created"))

(defn- create-admin
  []
  (api/create-user {:username "admin"
                    :password "admin"
                    :email    "admin@admin.com"
                    :role     "admin"})
  (println "Default admin user created"))

(defn- verify-initial-data
  []
  (when (nil? (db/get-secret))
    (create-secret))
  (when (empty? (db/users))
    (create-admin)))

(defn single-node-setup
  []
  (db/create-sns-users)
  (db/create-sns-replicator)
  (db/create-sns-global-changes)
  (println "Single node setup finished"))

(def migrations
  {:single-node-setup single-node-setup
   :initial           verify-initial-data})

(defn migrate
  []
  (doseq [migration (->> (apply dissoc migrations (db/migrations))
                         (into []))]
    (db/record-migration (key migration) ((val migration)))))