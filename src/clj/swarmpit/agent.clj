(ns swarmpit.agent
  (:require [immutant.scheduling :refer :all]
            [swarmpit.api :as api]))

(defn- autoredeploy-job
  []
  (let [services (->> (api/services)
                      (filter #(get-in % [:deployment :autoredeploy])))]
    (println (str "Autoredeploy agent checking for updates. Service checked: " (count services)))
    (doseq [service services]
      (let [id (:id service)
            repository (:repository service)
            registry (:registry service)
            current-image-id (api/service-image-id repository)
            latest-image-id (api/service-image-latest-id repository registry)]
        (when (not= current-image-id
                    latest-image-id)
          (do
            (api/update-service id service true)
            (println (str "Service " id " autoredeploy fired! [" current-image-id "] -> [" latest-image-id "]"))))))))

(defn init
  []
  (schedule autoredeploy-job
            (-> (in 1 :minutes)
                (every 60 :second))))