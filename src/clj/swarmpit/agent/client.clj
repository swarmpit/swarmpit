(ns swarmpit.agent.client
  (:require [swarmpit.http :refer :all]
            [swarmpit.config :refer [config]]
            [cheshire.core :refer [generate-string]]))

(defn- execute
  [{:keys [method ip api options]}]
  (let [agent-url (config :agent-url)
        agent-dynamic-url (str "http://" ip ":8080")]
    (execute-in-scope {:method        method
                       :url           (str (if agent-url agent-url agent-dynamic-url) api)
                       :options       options
                       :scope         "Agent"
                       :error-handler #(or (:detail %) %)})))

(defn info
  [agent-ip]
  (-> (execute {:method :GET
                :api    "/"
                :ip     agent-ip})
      :body))

(defn logs
  [agent-ip container-id since]
  (try
    (-> (execute {:method  :GET
                  :api     (str "/logs/" container-id)
                  :ip      agent-ip
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
