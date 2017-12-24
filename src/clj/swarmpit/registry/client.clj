(ns swarmpit.registry.client
  (:refer-clojure :exclude [get])
  (:import (clojure.lang ExceptionInfo))
  (:require [clj-http.client :as http]
            [clojure.walk :refer [keywordize-keys stringify-keys]]
            [clojure.string :as str]
            [swarmpit.http :refer :all]
            [swarmpit.token :as token]))

(defn- build-url
  [registry api]
  (str (:url registry) "/v2" api))

(defn- basic-auth
  [registry]
  (when (:withAuth registry)
    {"Authorization" (token/generate-basic (:username registry)
                                           (:password registry))}))

(defn- authenticate-header
  [headers]
  (let [www-authenticate (:www-authenticate headers)]
    (when www-authenticate
      (keywordize-keys
        (into (sorted-map)
              (map #(str/split % #"=")
                   (-> www-authenticate
                       (str/split #" ")
                       (second)
                       (str/replace "\"" "")
                       (str/split #","))))))))

(defn- execute
  [call]
  (execute-in-scope {:call-fx       call
                     :scope         "Registry"
                     :timeout       5000
                     :error-handler #(-> % :errors (first) :message)}))

(defn- fallback-options
  [www-auth-header options]
  (let [www-auth-url (:realm www-auth-header)
        www-auth-params (dissoc www-auth-header :realm)
        query-params (merge {"client_id" "swarmpit"} (stringify-keys www-auth-params))
        options (assoc-in options [:query-params] query-params)
        token (execute #(http/get www-auth-url options))]
    (-> options
        (assoc-in [:headers "Authorization"] (token/bearer (:token token))))))

(defn- execute-with-fallback
  [url options]
  (try
    (execute #(http/get url options))
    (catch ExceptionInfo e
      (let [status (:status (ex-data e))
            headers (:headers (ex-data e))
            www-auth-header (authenticate-header headers)]
        (if (and (= status 401)
                 (some? www-auth-header))
          (execute #(http/get url (fallback-options www-auth-header options)))
          (throw e))))))

(defn- get
  [registry api headers params]
  (let [url (build-url registry api)
        options {:headers      (merge {"Content-Type" "application/json"}
                                      headers)
                 :query-params params
                 :insecure?    true}]
    (execute-with-fallback url options)))

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