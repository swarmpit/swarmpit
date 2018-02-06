(ns swarmpit.database
  (:require [clojure.tools.logging :as log]
            [swarmpit.couchdb.client :as cc]
            [swarmpit.couchdb.migration :refer [migrate]]))

(defn- db-ready?
  []
  (try (cc/db-version)
       (catch Exception _ false)))

(defn- wait-for-db
  [sec]
  (log/info "Waiting for DB...")
  (loop [n sec]
    (if (db-ready?)
      (log/info "... connected after" (- sec n) "sec")
      (if (zero? n)
        (do
          (log/error "... timeout")
          (throw (ex-info "DB timeout" nil)))
        (do
          (Thread/sleep 1000)
          (recur (dec n)))))))

(defn- create-database
  []
  (try
    (cc/create-database)
    (catch Exception ex
      (:body (ex-data ex)))))

(defn init
  []
  (wait-for-db 100)
  (when (not (:error (create-database)))
    (log/info "DB schema created"))
  (migrate))