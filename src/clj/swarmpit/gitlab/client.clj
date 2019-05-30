(ns swarmpit.gitlab.client
  (:import (clojure.lang ExceptionInfo))
  (:require [swarmpit.http :refer :all]))

(defn- execute
  [{:keys [method url api options]}]
  (execute-in-scope {:method        method
                     :url           (str url api)
                     :options       (merge {:insecure? true} options)
                     :scope         "Gitlab"
                     :error-handler #(or (:error %) (:message %))}))

(defn groups
  [registry]
  (-> (execute {:method  :GET
                :url     (:gitlabUrl registry)
                :api     "/api/v4/groups"
                :options {:headers {:Private-Token (:token registry)}}})
      :body))

(defn group-projects
  [registry group-id]
  (-> (execute {:method  :GET
                :url     (:gitlabUrl registry)
                :api     (str "/api/v4/groups/" group-id "/projects")
                :options {:query-params {:simple true}
                          :headers      {:Private-Token (:token registry)}}})
      :body))

(defn projects
  [registry]
  (-> (execute {:method  :GET
                :url     (:gitlabUrl registry)
                :api     "/api/v4/projects"
                :options {:query-params {:membership true
                                         :simple     true}
                          :headers      {:Private-Token (:token registry)}}})
      :body))

(defn project-repositories
  [registry project-id]
  (try
    (-> (execute {:method  :GET
                  :url     (:gitlabUrl registry)
                  :api     (str "/api/v4/projects/" project-id "/registry/repositories")
                  :options {:headers {:Private-Token (:token registry)}}})
        :body)
    (catch ExceptionInfo _
      [])))