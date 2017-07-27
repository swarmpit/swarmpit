(ns swarmpit.docker.http
  (:require [clj-http.conn-mgr :as conn-mgr]
            [clj-http.client :as client]
            [cheshire.core :refer [parse-string generate-string]]
            [swarmpit.config :refer [config]])
  (:import (org.apache.http.conn.socket PlainConnectionSocketFactory)
           (org.apache.http.config RegistryBuilder)
           (swarmpit.socket UnixSocketFactory)
           (org.apache.http.impl.conn BasicHttpClientConnectionManager)))

(defn- http?
  [] (not (nil? (re-matches #"^https?:\/\/.*" (config :docker-sock)))))

(def ^:private http-scheme conn-mgr/regular-scheme-registry)

(defn- unix-scheme
  []
  (-> (RegistryBuilder/create)
      (.register "http" (UnixSocketFactory/createUnixSocketFactory (config :docker-sock)))
      (.build)))

(defn- make-conn-manager
  []
  (let [scheme (if (http?) http-scheme (unix-scheme))]
    (BasicHttpClientConnectionManager. scheme)))

(def ^:private get-function
  {"GET"    client/get
   "POST"   client/post
   "PUT"    client/put
   "DELETE" client/delete})

(defn- url
  [uri] (str (if (http?) (config :docker-sock) "http://localhost") uri))

(defn execute
  "Execute docker command and parse result"
  [method uri params headers payload]
  (let [response ((get-function method)
                   (url uri)
                   {:connection-manager (make-conn-manager)
                    :headers            headers
                    :query-params       params
                    :body               (generate-string payload)})
        body (-> response :body (parse-string true))
        code (-> response :status)]
    (if (> 400 code)
      body
      (throw (ex-info (str "Docker engine error: " (:message body))
                      {:status code
                       :body   {:error (:message body)}})))))


