(ns swarmpit.agent.client
  (:require [swarmpit.http :refer :all]
            [swarmpit.ip :refer [is-valid-url]]
            [swarmpit.config :refer [config]]
            [cheshire.core :refer [generate-string]]))

(defn- execute
  [{:keys [method url api options quiet-statuses]}]
  (execute-in-scope {:method          method
                     :url             (str url api)
                     :options         options
                     :scope           "Agent"
                     :error-handler   #(or (:detail %) %)
                     :quiet-statuses  quiet-statuses}))

(defn info
  [agent-url]
  (-> (execute {:method :GET
                :api    "/"
                :url    agent-url})
      :body))

(defn logs
  [agent-url container-id since]
  (when (not-empty container-id)
    (try
      (-> (execute {:method          :GET
                    :api             (str "/logs/" container-id)
                    :url             agent-url
                    :quiet-statuses  #{400 404}
                    :options         {:query-params
                                      (merge {}
                                             (when since
                                               {:since since}))}})
          :body)
      (catch Exception ex
        (let [e (ex-data ex)]
          (if (.contains [400 404] (:status e))
            nil
            (throw ex)))))))
