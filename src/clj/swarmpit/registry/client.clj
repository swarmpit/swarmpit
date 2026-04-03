(ns swarmpit.registry.client
  (:import (clojure.lang ExceptionInfo))
  (:require [clojure.walk :refer [keywordize-keys stringify-keys]]
            [clojure.string :as str]
            [swarmpit.http :refer :all]
            [swarmpit.token :as token]
            [swarmpit.ip :as ip]))

(defn- build-url
  [registry api]
  (if (:customApi registry)
    (str (:url registry) api)
    (str (:url registry) "/v2" api)))

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
  [{:keys [method url options quiet-statuses]}]
  (execute-in-scope {:method          method
                     :url             url
                     :options         (merge {:insecure? true} options)
                     :scope           "Registry"
                     :timeout         5000
                     :error-handler   #(-> % :errors (first) :message)
                     :quiet-statuses  quiet-statuses}))

(defn- repository-scope-from-url
  "Extract repository scope from Docker V2 API URL.
   GHCR returns a generic placeholder scope in www-authenticate headers,
   so we derive the correct scope from the actual request URL."
  [url]
  (when-let [[_ repo] (re-find #"/v2/(.+?)(?:/(?:tags|manifests|blobs)/)" url)]
    (str "repository:" repo ":pull")))

(defn- fallback-options
  [www-auth-url www-auth-params options]
  (let [query-params (merge {:client_id "swarmpit"} www-auth-params)
        token-options (assoc options :query-params query-params)
        token-body (-> (execute {:method  :GET
                                 :url     www-auth-url
                                 :options token-options})
                       :body)
        bearer-token (or (:token token-body) (:access_token token-body))]
    (assoc-in options [:headers :Authorization] (token/bearer bearer-token))))

(defn- execute-with-fallback
  [{:keys [method url options quiet-statuses] :as request}]
  (try
    (execute request)
    (catch ExceptionInfo e
      (let [status (:status (ex-data e))
            headers (:headers (ex-data e))
            www-auth-header (authenticate-header headers)
            www-auth-url (:realm www-auth-header)
            www-auth-params (dissoc www-auth-header :realm)
            url-scope (repository-scope-from-url url)
            www-auth-params (cond-> www-auth-params
                              url-scope (assoc :scope url-scope))]
        (if (and (= status 401)
                 (some? www-auth-url)
                 (ip/is-valid-url www-auth-url))
          (-> request
              (assoc :options (fallback-options www-auth-url www-auth-params options))
              (execute))
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

(defn- request-manifest
  [registry repository-name repository-tag method type]
  (let [response (execute-with-fallback {:method          method
                                         :url             (build-url registry (str "/" repository-name "/manifests/" repository-tag))
                                         :options         {:headers (merge (basic-auth registry)
                                                                           {:Accept type})}
                                         :quiet-statuses  #{404}})
        response-type (get-in response [:headers :content-type])]
    (when (= type response-type) response)))

(defn manifest
  [registry repository-name repository-tag]
  (:body (request-manifest registry repository-name repository-tag :GET
                           "application/vnd.docker.distribution.manifest.v1+prettyjws")))

(defn digest
  [registry repository-name repository-tag]
  (-> (or (request-manifest registry repository-name repository-tag :HEAD
                            "application/vnd.docker.distribution.manifest.list.v2+json")
          (request-manifest registry repository-name repository-tag :HEAD
                            "application/vnd.docker.distribution.manifest.v2+json"))
      :headers
      :docker-content-digest))
