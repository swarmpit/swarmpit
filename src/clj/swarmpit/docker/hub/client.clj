(ns swarmpit.docker.hub.client
  (:require [swarmpit.http :refer :all]
            [cheshire.core :refer [generate-string]]))

(def ^:private base-url "https://hub.docker.com/v2")

(defn- execute
  [{:keys [method api options]}]
  (execute-in-scope {:method        method
                     :url           (str base-url api)
                     :options       options
                     :scope         "Dockerhub"
                     :error-handler #(or (:detail %) %)}))

(defn- jwt-auth
  [token]
  {:Authorization (str "JWT " token)})

(defn login
  [user]
  (-> (execute {:method  :POST
                :api     "/users/login"
                :options {:body    (select-keys user [:username :password])
                          :headers {:Content-Type "application/json"}}})
      :body))

(defn info
  [user]
  (-> (execute {:method :GET
                :api    (str "/users/" (:username user))})
      :body))

(defn repositories-by-namespace
  [token namespace]
  (-> (execute {:method  :GET
                :api     (str "/repositories/" namespace)
                :options {:query-params {:page_size 1000}
                          :headers      (jwt-auth token)}})
      :body))

(defn namespaces
  [token]
  (-> (execute {:method  :GET
                :api     "/repositories/namespaces"
                :options {:headers (jwt-auth token)}})
      :body))

(defn repositories
  [query page]
  (-> (execute {:method  :GET
                :api     "/search/repositories"
                :options {:query-params {:query     query
                                         :page      page
                                         :page_size 20}}})
      :body))