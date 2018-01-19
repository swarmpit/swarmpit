(ns swarmpit.agent
  (:import (clojure.lang ExceptionInfo))
  (:require [clojure.tools.logging :as log]
            [org.httpkit.timer :refer [schedule-task]]
            [swarmpit.api :as api]))

(defn- autoredeploy-job
  []
  (let [services (->> (api/services)
                      (filter #(get-in % [:deployment :autoredeploy]))
                      (filter #(not (= "updating" get-in % [:status :update]))))]
    (log/debug "Autoredeploy agent checking for updates. Services to be checked:" (count services))
    (doseq [service services]
      (let [id (:id service)
            repository (:repository service)]
        (log/info repository)
        (try
          (let [current-digest (:imageDigest repository)
                latest-digest (api/repository-digest nil (:name repository) (:tag repository))]
            (when (not= current-digest
                        latest-digest)
              (api/redeploy-service nil id)
              (log/info "Service" id "has been redeployed! [" current-digest "] -> [" latest-digest "]")))
          (catch ExceptionInfo e
            (log/error "Service" id "autoredeploy failed! " (dissoc (ex-data e) :headers))))))))

(defn init []
  (schedule-task 60000
                 (try
                   (autoredeploy-job)
                   (catch Exception ex
                     (log/error "Autoredeploy execution failure:" ex)))
                 (init)))