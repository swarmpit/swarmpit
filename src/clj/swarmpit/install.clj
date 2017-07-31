(ns swarmpit.install
  (:require [swarmpit.couchdb.client :as cc]
            [swarmpit.api :as api]
            [swarmpit.uuid :as uuid]))

(defn- create-database
  []
  (try
    (cc/create-database)
    (catch Exception ex
      (get-in (ex-data ex) [:body]))))

(defn- create-secret
  []
  (cc/create-secret {:secret (uuid/uuid)
                     :type   "secret"})
  (println "Default token secret created"))

(defn- create-admin
  []
  (api/create-user {:username "admin"
                    :password "admin"
                    :email    "admin@admin.com"
                    :role     "admin"})
  (println "Default admin user created"))

(defn init
  []
  (cc/db-version)
  (when (not (:error (create-database)))
    (println "DB schema created"))
  (when (nil? (cc/get-secret))
    (create-secret))
  (when (empty? (cc/users))
    (create-admin)))