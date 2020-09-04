(ns swarmpit.github.client
  (:require [swarmpit.http :refer :all]
            [graphql-builder.parser :refer [defgraphql]]
            [graphql-builder.core :as core]))

(defgraphql graphql-queries
            "graphql/github/user_packages.graphql"
            "graphql/github/org_packages.graphql"
            "graphql/github/delete_package.graphql")

(defn- graphql-response
  [result]
  (let [result-keys (set (keys result))]
    (if (contains? result-keys :errors)
      (let [error (first (:errors result))]
        (throw
          (ex-info (str "Github error: " (:message error))
                   {:status 400
                    :type   :http-client
                    :body   {:error (:message error)}})))
      (:data result))))

(def query-map (core/query-map graphql-queries))

(defn- execute
  [{:keys [method url api options]}]
  (execute-in-scope {:method        method
                     :url           (str url api)
                     :options       (merge {:insecure? true} options)
                     :scope         "Github"
                     :error-handler #(or (:error %) (:message %))}))

(defn orgs
  [registry]
  (-> (execute {:method  :GET
                :url     (:githubUrl registry)
                :api     "/user/orgs"
                :options {:headers {:Authorization (str "token " (:token registry))}}})
      :body))

(def org-packages-query (get-in query-map [:query :org-packages]))

(defn org-packages
  [registry org]
  (let [query (org-packages-query {:login org})]
    (-> (execute {:method  :POST
                  :url     (:githubUrl registry)
                  :api     "/graphql"
                  :options {:headers {:Authorization (str "bearer " (:token registry))}
                            :body    (:graphql query)}})
        :body
        (graphql-response))))

(def user-packages-query (get-in query-map [:query :user-packages]))

(defn user-packages
  [registry]
  (let [query (user-packages-query {:login (:username registry)})]
    (-> (execute {:method  :POST
                  :url     (:githubUrl registry)
                  :api     "/graphql"
                  :options {:headers {:Authorization (str "bearer " (:token registry))}
                            :body    (:graphql query)}})
        :body
        (graphql-response))))

(def delete-package-mutation (get-in query-map [:mutation :delete-package]))

(defn delete-package
  [registry id]
  (let [mutation (delete-package-mutation {:id id})]
    (-> (execute {:method  :POST
                  :url     (:githubUrl registry)
                  :api     "/graphql"
                  :options {:headers {:Authorization (str "bearer " (:token registry))
                                      :Accept        "application/vnd.github.package-deletes-preview+json"}
                            :body    (:graphql mutation)}})
        :body
        (graphql-response))))