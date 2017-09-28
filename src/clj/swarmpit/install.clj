(ns swarmpit.install
  (:require [swarmpit.couchdb.client :as cc]
            [swarmpit.couchdb.migration :refer [migrate]]))

(defn- db-ready?
  []
  (try (cc/db-version)
       (catch Exception ex false)))

(defn- wait-for-db
  [sec]
  (println "Waiting for DB...")
  (loop [n sec]
    (if (db-ready?)
      (println (str "... connected after " (- sec n) " sec"))
      (if (zero? n)
        (do
          (println "... timeout")
          (throw (ex-info "DB timeout" nil)))
        (do
          (Thread/sleep 1000)
          (recur (dec n)))))))

(defn- create-database
  []
  (try
    (cc/create-database)
    (catch Exception ex
      (get-in (ex-data ex) [:body]))))

(defn init
  []
  (wait-for-db 100)
  (when (not (:error (create-database)))
    (println "DB schema created"))
  (migrate))