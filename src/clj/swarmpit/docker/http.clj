(ns swarmpit.docker.http
  (:refer-clojure :exclude [get])
  (:require [clj-http.conn-mgr :as conn-mgr]
            [clj-http.client :as client]
            [cheshire.core :refer [parse-string generate-string]]
            [swarmpit.config :refer [config]])
  (:import (org.apache.http.conn.socket PlainConnectionSocketFactory)
           (org.apache.http.config RegistryBuilder)
           (swarmpit.socket UnixSocketFactory)
           (org.apache.http.impl.conn BasicHttpClientConnectionManager)
           (java.io IOException)
           (clojure.lang ExceptionInfo)))

(defn- http?
  [] (not (nil? (re-matches #"^https?:\/\/.*" (config :docker-sock)))))

(defn- unix-scheme
  []
  (-> (RegistryBuilder/create)
      (.register "http" (UnixSocketFactory/createUnixSocketFactory (config :docker-sock)))
      (.build)))

(defn- make-conn-manager
  []
  (let [scheme (if (http?)
                 conn-mgr/regular-scheme-registry
                 (unix-scheme))]
    (BasicHttpClientConnectionManager. scheme)))

(defn- url
  [uri]
  (let [server (if (http?)
                 (config :docker-sock)
                 "http://localhost")
        api (str "/v" (config :docker-api))]
    (str server api uri)))

(def ^:private func-map
  {"GET"    client/get
   "POST"   client/post
   "PUT"    client/put
   "DELETE" client/delete})

(defn execute
  "Execute docker command and parse result"
  [method uri params headers payload]
  (try
    (let [response ((func-map method)
                     (url uri)
                     {:connection-manager (make-conn-manager)
                      :headers            headers
                      :query-params       params
                      :body               (generate-string payload)
                      :retry-handler      (fn [& args] false)})
          body (-> response :body (parse-string true))
          code (-> response :status)]
      body)
    (catch IOException exception
      (throw (let [error (.getMessage exception)]
               (ex-info (str "Docker failure: " error) {:message error}))))
    (catch ExceptionInfo exception
      (throw (let [data (some-> exception (ex-data))
                   error (if (= "application/json" ((:headers data) "Content-Type"))
                           (parse-string (:body data) true)
                           {:message (:reason-phrase data)})]
               (ex-info (str "Docker error: " (:message error)) error))))))

(defn get
  ([uri] (get uri nil nil))
  ([uri params] (get uri params nil))
  ([uri params headers]
   (execute "GET" uri params headers nil)))

(defn post
  ([uri payload] (post uri nil nil payload))
  ([uri params payload] (post uri params nil payload))
  ([uri params headers payload]
   (execute "POST" uri params (merge headers {:Content-Type "application/json"}) payload)))

(defn put
  ([uri payload] (put uri nil nil payload))
  ([uri headers payload] (put uri nil headers payload))
  ([uri params headers payload]
   (execute "PUT" uri params (merge headers {:Content-Type "application/json"}) payload)))

(defn delete
  ([uri] (delete uri nil))
  ([uri headers]
   (execute "DELETE" uri nil headers nil)))
