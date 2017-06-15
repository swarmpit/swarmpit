(ns swarmpit.install
  (:require [swarmpit.couchdb.client :as cc]
            [swarmpit.api :as api]))

(defn- create-database
  []
  (try
    (cc/create-database)
    (catch Exception ex
      (get-in (ex-data ex) [:body]))))

(defn- create-admin
  []
  (api/create-user {:username "admin"
                    :password "admin"
                    :email    "admin@admin.com"
                    :role     "admin"})
  (println "Default admin user created"))

(defn init
  []
  (println "Swarmpit is starting...")
  (cc/db-version)
  (if (not (:error (create-database)))
    (println "DB schema created"))
  (if (empty? (cc/users))
    (create-admin)))