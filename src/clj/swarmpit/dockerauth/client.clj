(ns swarmpit.dockerauth.client
  (:refer-clojure :exclude [get])
  (:require [org.httpkit.client :as http]
            [swarmpit.token :as token]
            [swarmpit.http :refer :all]
            [swarmpit.docker-utils :as utils]))

(def ^:private base-url "https://auth.docker.io")

(defn- execute [call] (execute-in-scope call "Docker auth" :details))

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
                :scope   (str "repository:" (utils/add-dockerhub-namespace repository) ":pull")}]
    (get "/token" headers params)))
