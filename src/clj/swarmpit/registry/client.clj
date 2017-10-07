(ns swarmpit.registry.client
  (:refer-clojure :exclude [get])
  (:import  (clojure.lang ExceptionInfo))
  (:require [org.httpkit.client :as http]
            [clojure.string :as str]
            [swarmpit.http :refer :all]
            [swarmpit.token :as token]))

(defn- build-url
  [registry api]
  (str (:url registry) "/v2" api))

(defn- execute [call] (execute-in-scope call "Registry" #(-> % :errors (first) :message)))

(defn- basic-auth
  [registry]
  (when (:withAuth registry)
    {"Authorization" (token/generate-basic (:username registry)
                                           (:password registry))}))

(defn- bearer-auth-header
  [token]
  {"Authorization" (str "Bearer " token)})

(defn- get
  [registry api headers params]
  (let [url (build-url registry api)
        options {:timeout      5000
                 :headers      (merge {"Content-Type" "application/json"}
                                      headers)
                 :query-params params
                 :insecure?    true}]
    (try
      (execute @(http/get url options))
      (catch ExceptionInfo e
        (if
          (and
            (=  (:status (ex-data e)) 401)
            (some? (:www-authenticate (:headers (ex-data e)))))
          (let [token (obtain-jwt-token (:www-authenticate (:headers (ex-data e))) registry)]
            (let [options-with-token (assoc options :headers (merge (:headers options) (bearer-auth-header token)))]
              (execute @(http/get url options-with-token))))
          (throw e))))))

(defn- parse-authenticate-header
  [www-authenticate]
  (clojure.walk/keywordize-keys
    (into (sorted-map)
      (map #(str/split % #"=")
        (-> www-authenticate
          (str/split #" ")
          (second)
          (str/replace "\"" "")
          (str/split #","))))))

(defn- obtain-jwt-token
  [www-authenticate registry]
  (let [params (parse-authenticate-header www-authenticate)
        auth-header (basic-auth registry)]
    (let [url (:realm params)
          options {
                    :timeout      5000
                    :debug true
                    :headers      (merge {"Content-Type" "application/json"
                                          "Accept" "application/json"} auth-header)
                    :query-params (merge {"client_id" "swarmpit"} (clojure.walk/stringify-keys (dissoc params :realm)))
                    :insecure?    true}]
      (:token (execute @(http/get url options))))))


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