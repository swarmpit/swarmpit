(ns swarmpit.dockerregistry.client
  (:refer-clojure :exclude [get])
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string generate-string]]))

(def ^:private base-url "https://index.docker.io/v2")

(defn- execute
  [call-fx]
  (let [{:keys [status body error]} call-fx]
    (if error
      (throw
        (ex-info "Docker registry client failure!"
                 {:status 500
                  :body   {:error (:cause (Throwable->map error))}}))
      (let [response (parse-string body true)]
        (if (> 400 status)
          response
          (throw
            (ex-info "Docker registry error!"
                     {:status status
                      :body   response})))))))

(defn- get
  [api token headers params]
  (let [url (str base-url api)
        options {:headers      (merge headers
                                      {"Authorization" (str "Bearer " token)})
                 :query-params params}]
    (execute @(http/get url options))))

(defn tags
  [token repository]
  (let [api (str "/" repository "/tags/list")]
    (get api token {} nil)))

(defn manifest
  [token repository-name repository-tag]
  (let [api (str "/" repository-name "/manifests/" repository-tag)]
    (get api token {"Accept" "application/vnd.docker.distribution.manifest.v2+json"} nil)))
