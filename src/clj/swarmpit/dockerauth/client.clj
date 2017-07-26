(ns swarmpit.dockerauth.client
  (:refer-clojure :exclude [get])
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string generate-string]]
            [swarmpit.token :as token]
            [swarmpit.repository :as repo]))

(def ^:private base-url "https://auth.docker.io")

(defn- format-repository
  [repository]
  (if (repo/namespace? repository)
    repository
    (str "library/" repository)))

(defn- execute
  [call-fx]
  (let [{:keys [status body error]} call-fx]
    (if error
      (throw
        (ex-info "Docker auth client failure!"
                 {:status 500
                  :body   {:error (:cause (Throwable->map error))}}))
      (let [response (parse-string body true)]
        (if (> 400 status)
          response
          (throw
            (ex-info "Docker auth error!"
                     {:status status
                      :body   {:error response}})))))))

(defn- get
  [api headers params]
  (let [url (str base-url api)
        options {:headers      headers
                 :query-params params}]
    (execute @(http/get url options))))

(defn- basic-auth
  [user]
  (when (some? user)
    {"Authorization" (token/generate-basic (:username user)
                                           (:password user))}))

(defn token
  [user repository]
  (let [headers (basic-auth user)
        params {:service "registry.docker.io"
                :scope   (str "repository:" (format-repository repository) ":pull")}]
    (get "/token" headers params)))
