(ns swarmpit.gitlab.client
  (:require [swarmpit.http :refer :all]))

(defn- execute
  [{:keys [method url api options]}]
  (execute-in-scope {:method        method
                     :url           (str url api)
                     :options       options
                     :scope         "Gitlab"
                     :error-handler #(or (:error %) (:message %))}))

(defn projects
  [registry]
  (-> (execute {:method  :GET
                :url     (:gitlabUrl registry)
                :api     (str "/api/v4/users/" (:username registry) "/projects")
                :options {:headers {:Private-Token (:token registry)}}})
      :body))

(defn project-repositories
  [registry project-id]
  (-> (execute {:method  :GET
                :url     (:gitlabUrl registry)
                :api     (str "/api/v4/projects/" project-id "/registry/repositories")
                :options {:headers {:Private-Token (:token registry)}}})
      :body))