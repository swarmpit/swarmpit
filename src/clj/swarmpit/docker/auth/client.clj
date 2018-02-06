(ns swarmpit.docker.auth.client
  (:require [swarmpit.token :as token]
            [swarmpit.http :refer :all]))

(def ^:private base-url "https://auth.docker.io")

(defn- execute
  [{:keys [method api options]}]
  (execute-in-scope {:method        method
                     :url           (str base-url api)
                     :options       options
                     :scope         "Docker auth"
                     :error-handler :details}))

(defn- basic-auth
  [user]
  (when (some? user)
    {:Authorization (token/generate-basic (:username user)
                                          (:password user))}))

(defn token
  [user repository]
  (let [query-params {:service "registry.docker.io"
                      :scope   (str "repository:" repository ":pull")}]
    (-> (execute {:method  :GET
                  :api     "/token"
                  :options {:headers      (basic-auth user)
                            :query-params query-params}})
        :body)))
