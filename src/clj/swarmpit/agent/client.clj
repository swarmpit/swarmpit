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
                :url    agent-url})
      :body))

(defn logs
  [agent-url container-id since]
  (-> (execute {:method  :GET
                :api     (str "/logs/" container-id)
                :url     agent-url
                :options {:query-params
                          (merge {}
                                 (when since
                                   {:since since}))}})
      :body))
