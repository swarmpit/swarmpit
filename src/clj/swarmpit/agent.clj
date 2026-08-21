(ns swarmpit.agent
  (:require [clojure.string :as str]
            [swarmpit.api :as api]
            [chime.core :as chime]
            [taoensso.timbre :refer [info debug error]])
  (:import (clojure.lang ExceptionInfo)
           (java.time Instant Duration)))

(def ^:private in-flight-update-states #{"updating" "rollback_started"})

(defn- autoredeploy-job
  []
  (let [services (->> (api/services)
                      (filter #(get-in % [:deployment :autoredeploy]))
                      (remove #(contains? in-flight-update-states (get-in % [:status :update]))))]
    (doseq [service services]
      (let [id (:id service)
            name (:serviceName service)
            repository (:repository service)]
        (try
          (let [current-digest (:imageDigest repository)
                ;; same normalisation redeploy-service applies, otherwise
                ;; untagged images 404 on every poll
                tag (api/standardize-repository-tag (:tag repository))
                latest-digest (api/repository-digest nil (:name repository) tag)]
            (cond
              ;; An unresolvable upstream digest compared unequal on every poll,
              ;; so the service got force-redeployed once a minute forever and
              ;; its spec version never settled long enough to accept an api
              ;; update. Do nothing until we can actually read a digest.
              (str/blank? latest-digest)
              (debug "Service" id (str "(" name ")") "autoredeploy skipped, upstream digest unresolved")

              (not= current-digest latest-digest)
              (do
                (api/redeploy-service nil id nil latest-digest)
                (info "Service" id (str "(" name ")") "autoredeploy fired! DIGEST:" (str "[" current-digest "] -> [" latest-digest "]")))))
          (catch ExceptionInfo e
            (error "Service" id (str "(" name ")") "autoredeploy failed!" (ex-data e)))
          (catch Exception e
            (error "Service" id (str "(" name ")") "autoredeploy failed!" (.getMessage e))))))))

(defn init []
  (let [start (.plusSeconds (Instant/now) 60)]
    (chime/chime-at
      (chime/periodic-seq start (Duration/ofMinutes 1))
      (fn [time]
        (autoredeploy-job)))))