(ns swarmpit.agent
  (:require [swarmpit.api :as api]
            [chime.core :as chime]
            [taoensso.timbre :refer [info debug error]])
  (:import (clojure.lang ExceptionInfo)
           (java.time Instant Duration)))

(defn- autoredeploy-job
  []
  (let [services (->> (api/services)
                      (filter #(get-in % [:deployment :autoredeploy]))
                      (filter #(not (= "updating" get-in % [:status :update]))))]
    (doseq [service services]
      (let [id (:id service)
            name (:serviceName service)
            repository (:repository service)]
        (try
          (let [current-digest (:imageDigest repository)
                latest-digest (api/repository-digest nil
                                                     (:name repository)
                                                     (:tag repository))]
            (when (not= current-digest
                        latest-digest)
              (api/redeploy-service nil id nil)
              (info "Service" id (str "(" name ")") "autoredeploy fired! DIGEST:" (str "[" current-digest "] -> [" latest-digest "]"))))
          (catch ExceptionInfo e
            (error "Service" id (str "(" name ")") "autoredeploy failed!" (ex-data e))))))))

(defn init []
  (let [start (.plusSeconds (Instant/now) 60)]
    (chime/chime-at
      (chime/periodic-seq start (Duration/ofMinutes 1))
      (fn [time]
        (autoredeploy-job)))))