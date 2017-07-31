(ns swarmpit.registry.client
  (:refer-clojure :exclude [get])
  (:require [org.httpkit.client :as http]
            [swarmpit.response :as resp]
            [swarmpit.token :as token]))

(defn- build-url
  [registry api]
  (str (:url registry) "/v2" api))

(defn- execute
  [call-fx]
  (let [{:keys [status body error]} call-fx]
    (if error
      (throw
        (ex-info "Registry client failure!"
                 {:status 500
                  :body   {:error (:cause (Throwable->map error))}}))
      (let [response (resp/parse-response-body body)]
        (if (> 400 status)
          response
          (throw
            (ex-info "Registry error!"
                     {:status status
                      :body   response})))))))

(defn- get
  [registry api headers params]
  (let [url (build-url registry api)
        options {:timeout      5000
                 :headers      (merge {"Content-Type" "application/json"}
                                      headers)
                 :query-params params
                 :insecure?    true}]
    (execute @(http/get url options))))

(defn- basic-auth
  [registry]
  (when (:withAuth registry)
    {"Authorization" (token/generate-basic (:username registry)
                                           (:password registry))}))

(defn repositories
  [registry]
  (let [headers (basic-auth registry)]
    (->> (get registry "/_catalog" headers nil)
         :repositories)))

(defn info
  [registry]
  (let [headers (basic-auth registry)]
    (get registry "/" headers nil)))

(defn tags
  [registry repository-name]
  (let [headers (basic-auth registry)
        api (str "/" repository-name "/tags/list")]
    (get registry api headers nil)))

(defn manifest
  [registry repository-name repository-tag]
  (let [headers (basic-auth registry)
        api (str "/" repository-name "/manifests/" repository-tag)]
    (get registry api headers nil)))

(defn distribution
  [registry repository-name repository-tag]
  (let [headers (basic-auth registry)
        api (str "/" repository-name "/manifests/" repository-tag)]
    (get registry api (merge headers
                             {"Accept" "application/vnd.docker.distribution.manifest.v2+json"}) nil)))