(ns swarmpit.agent.client
  (:require [swarmpit.http :refer :all]
            [swarmpit.config :refer [config]]
            [cheshire.core :refer [generate-string]]))

(defn- execute
  [{:keys [method url api options]}]
  (let [agent-url (config :agent-url)]
    (execute-in-scope {:method        method
                       :url           (str (if agent-url agent-url url) api)
                       :options       options
                       :scope         "Agent"
                       :error-handler #(or (:detail %) %)})))

(defn info
  [agent-url]
  (-> (execute {:method :GET
                :api    "/"
                :url    (str "http://" agent-url)})
      :body))

(defn logs
  [agent-url container-id since]
  (try
    (let [result (-> (execute {:method  :GET
                               :api     (str "/logs/" container-id)
                               :url     (str "http://" agent-url)
                               :options {:query-params
                                         (merge {}
                                                (when since
                                                  {:since since}))}})
                     :body)]
      (println result)
      result)
    (catch Exception ex
      (let [e (ex-data ex)]
        (print e)
        (if (.contains [400 404] (:status e))
          nil
          (throw ex))))))
