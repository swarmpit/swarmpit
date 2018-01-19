(ns swarmpit.registry.client
  (:import (clojure.lang ExceptionInfo))
  (:require [clojure.walk :refer [keywordize-keys stringify-keys]]
            [clojure.string :as str]
            [swarmpit.http :refer :all]
            [swarmpit.token :as token]))

(defn- build-url
  [registry api]
  (str (:url registry) "/v2" api))

(defn- basic-auth
  [registry]
  (when (:withAuth registry)
    {:Authorization (token/generate-basic (:username registry)
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
  [{:keys [method url options]}]
  (execute-in-scope {:method        method
                     :url           url
                     :options       (merge {:insecure? true} options)
                     :scope         "Registry"
                     :timeout       5000
                     :error-handler #(-> % :errors (first) :message)}))

(defn- fallback-options
  [www-auth-header options]
  (let [www-auth-url (:realm www-auth-header)
        www-auth-params (dissoc www-auth-header :realm)
        query-params (merge {:client_id "swarmpit"} www-auth-params)
        options (assoc-in options [:query-params] query-params)
        token (-> (execute {:method  :GET
                            :url     www-auth-url
                            :options options})
                  :body)]
    (-> options
        (assoc-in [:headers :Authorization] (token/bearer (:token token))))))

(defn- execute-with-fallback
  [{:keys [method url options] :as request}]
  (try
    (execute request)
    (catch ExceptionInfo e
      (let [status (:status (ex-data e))
            headers (:headers (ex-data e))
            www-auth-header (authenticate-header headers)]
        (if (and (= status 401)
                 (some? www-auth-header))
          (execute (assoc request :options (fallback-options www-auth-header options)))
          (throw e))))))

(defn repositories
  [registry]
  (-> (execute-with-fallback
        {:method  :GET
         :url     (build-url registry "/_catalog")
         :options {:headers (basic-auth registry)}})
      :body
      :repositories))

(defn info
  [registry]
  (-> (execute-with-fallback
        {:method  :GET
         :url     (build-url registry "/")
         :options {:headers (basic-auth registry)}})
      :body))

(defn tags
  [registry repository-name]
  (-> (execute-with-fallback
        {:method  :GET
         :url     (build-url registry (str "/" repository-name "/tags/list"))
         :options {:headers (basic-auth registry)}})
      :body))

(defn manifest
  [registry repository-name repository-tag]
  (-> (execute-with-fallback
        {:method  :GET
         :url     (build-url registry (str "/" repository-name "/manifests/" repository-tag))
         :options {:headers (basic-auth registry)}})
      :body))

(defn digest
  [registry repository-name repository-tag]
  (-> (execute-with-fallback
        {:method  :GET
         :url     (build-url registry (str "/" repository-name "/manifests/" repository-tag))
         :options {:headers (merge (basic-auth registry)
                                   {:Accept "application/vnd.docker.distribution.manifest.v2+json"})}})
      :headers
      :docker-content-digest))