(ns swarmpit.agent
  (:import (clojure.lang ExceptionInfo))
  (:require [immutant.scheduling :refer :all]
            [clojure.tools.logging :as log]
            [swarmpit.api :as api]))

(defn- autoredeploy-job
  []
  (let [services (->> (api/services)
                      (filter #(get-in % [:deployment :autoredeploy]))
                      (filter #(not (= "updating" get-in % [:status :update]))))]
    (log/debug "Autoredeploy agent checking for updates. Services to be checked:" (count services))
    (doseq [service services]
      (let [id (:id service)
            repository (:repository service)
            current-image-id (:imageId repository)]
        (try
          (let [latest-image-id (api/service-image-id service true)]
            (when (not= current-image-id
                        latest-image-id)
              (api/update-service id (-> service
                                         (assoc-in [:networks] (api/service-networks id))) true)
              (log/info "Service" id "has been redeployed! [" current-image-id "] -> [" latest-image-id "]")))
          (catch ExceptionInfo e
            (log/error "Service" id "autoredeploy failed! " (ex-data e))))))))

(defn init
  []
  (schedule autoredeploy-job
            (-> (in 1 :minutes)
                (every 60 :second))))