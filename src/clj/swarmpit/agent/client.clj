(ns swarmpit.agent.client
  (:require [swarmpit.http :refer :all]
            [swarmpit.ip :refer [is-valid-url]]
            [swarmpit.config :refer [config]]
            [cheshire.core :refer [generate-string]]))

(defn- execute
  [{:keys [method url api options]}]
  (execute-in-scope {:method        method
                     :url           (str url api)
                     :options       options
                     :scope         "Agent"
                     :error-handler #(or (:detail %) %)}))

(defn info
  [agent-url]
  (-> (execute {:method :GET
                :api    "/"
                :url    agent-url})
      :body))

(defn logs
  [agent-url container-id since]
  (try
    (-> (execute {:method  :GET
                  :api     (str "/logs/" container-id)
                  :url     agent-url
                  :options {:query-params
                            (merge {}
                                   (when since
                                     {:since since}))}})
        :body)
    (catch Exception ex
      (let [e (ex-data ex)]
        (if (.contains [400 404] (:status e))
          nil
          (throw ex))))))
