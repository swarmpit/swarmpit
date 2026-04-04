(ns swarmpit.docker.engine.http
  (:require [clj-http.conn-mgr :as conn-mgr]
            [swarmpit.http :refer :all]
            [cheshire.core :refer [parse-string generate-string]]
            [swarmpit.config :refer [config]]
            [swarmpit.utils :refer [parse-int]])
  (:import (org.apache.http.config RegistryBuilder SocketConfig)
           (swarmpit.socket UnixSocketFactory)
           (org.apache.http.impl.conn PoolingHttpClientConnectionManager)))

(defn- http?
  [] (not (nil? (re-matches #"^https?:\/\/.*" (config :docker-sock)))))

(defn- unix-scheme
  []
  (-> (RegistryBuilder/create)
      (.register "http" (UnixSocketFactory/createUnixSocketFactory (config :docker-sock)))
      (.build)))

(defn- make-pooling-unix-conn-manager
  []
  (let [socket-config (-> (SocketConfig/custom)
                          (.setSoTimeout 30000)
                          (.build))]
    (doto (PoolingHttpClientConnectionManager. (unix-scheme))
      (.setMaxTotal 50)
      (.setDefaultMaxPerRoute 50)
      (.setValidateAfterInactivity 1000)
      (.setDefaultSocketConfig socket-config))))

(defn make-conn-manager
  []
  (if (http?)
    (conn-mgr/make-reusable-conn-manager {:timeout 10 :threads 50 :default-per-route 50})
    (make-pooling-unix-conn-manager)))

(defonce ^:private shared-conn-manager (delay (make-conn-manager)))

(defn get-conn-manager
  "Returns shared connection manager. Exists as a function so tests
   can with-redefs to supply a fresh manager for different configs."
  []
  @shared-conn-manager)

(defn- url
  [uri]
  (let [server (if (http?)
                 (config :docker-sock)
                 "http://localhost")
        api (str "/v" (config :docker-api))]
    (str server api uri)))

(defn execute
  [{:keys [method api options]}]
  (let [timeout-ms (or (parse-int (config :docker-http-timeout)) 5000)]
    (execute-in-scope {:method        method
                       :url           (url api)
                       :options       (merge {:connection-manager          (get-conn-manager)
                                              :connection-request-timeout  timeout-ms
                                              :socket-timeout              timeout-ms
                                              :connection-timeout          (max timeout-ms 5000)
                                              :retry-handler               (fn [& _] false)} options)
                       :scope         "Docker"
                       :timeout       (+ timeout-ms 5000)
                       :error-handler :message})))
