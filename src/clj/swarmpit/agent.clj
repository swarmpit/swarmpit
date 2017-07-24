(ns swarmpit.agent
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
            registry (:registry service)
            current-image-id (api/service-image-id repository)
            latest-image-id (api/service-image-latest-id repository registry)]
        (when (not= current-image-id
                    latest-image-id)
          (api/update-service id service true)
          (log/info "Service" id "autoredeploy fired! [" current-image-id "] -> [" latest-image-id "]"))))))

(defn init
  []
  (schedule autoredeploy-job
            (-> (in 1 :minutes)
                (every 60 :second))))