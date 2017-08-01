(ns swarmpit.dockerregistry.client
  (:refer-clojure :exclude [get])
  (:require [org.httpkit.client :as http]
            [swarmpit.http :refer :all]
            [swarmpit.repository :as repo]
            [cheshire.core :refer [parse-string generate-string]]))

(def ^:private base-url "https://index.docker.io/v2")

(defn- execute
  [call]
  (execute-in-scope call "Docker registry" #(-> % :errors (first) :message)))

(defn- get
  [api token headers params]
  (let [url (str base-url api)
        options {:headers      (merge headers
                                      {"Authorization" (str "Bearer " token)})
                 :query-params params}]
    (execute @(http/get url options))))

(defn tags
  [token repository]
  (let [api (str "/" (repo/add-dockerhub-namespace repository) "/tags/list")]
    (get api token {} nil)))

(defn manifest
  [token repository-name repository-tag]
  (let [api (str "/" (repo/add-dockerhub-namespace repository-name) "/manifests/" repository-tag)]
    (get api token {"Accept" "application/vnd.docker.distribution.manifest.v2+json"} nil)))
