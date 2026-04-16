(ns swarmpit.docker.engine.http
  (:require [clj-http.conn-mgr :as conn-mgr]
            [swarmpit.http :refer :all]
            [cheshire.core :refer [parse-string generate-string]]
            [swarmpit.config :refer [config]]
            [swarmpit.utils :refer [parse-int]])
  (:import (org.apache.http.config RegistryBuilder SocketConfig)
           (swarmpit.socket UnixSocketFactory)
           (org.apache.http.impl.conn BasicHttpClientConnectionManager)))

(defn- http?
  [] (not (nil? (re-matches #"^https?:\/\/.*" (config :docker-sock)))))

(defn- unix-scheme
  []
  (-> (RegistryBuilder/create)
      (.register "http" (UnixSocketFactory/createUnixSocketFactory (config :docker-sock)))
      (.build)))

(defn- make-unix-conn-manager
  []
  (let [socket-config (-> (SocketConfig/custom)
                          (.setSoTimeout 30000)
                          (.build))]
    (doto (BasicHttpClientConnectionManager. (unix-scheme))
      (.setSocketConfig socket-config))))

(defn make-conn-manager
  []
  (if (http?)
    (conn-mgr/make-regular-conn-manager {})
    (make-unix-conn-manager)))

(defn get-conn-manager
  "Creates a fresh per-request connection manager.
   Per-request managers avoid pool exhaustion when future-cancel
   doesn't properly close Unix sockets on timeout."
  []
  (make-conn-manager))

(defn- url
  [uri]
  (let [server (if (http?)
                 (config :docker-sock)
                 "http://localhost")
        api (str "/v" (config :docker-api))]
    (str server api uri)))

(defn execute
  [{:keys [method api options]}]
  (let [timeout-ms (or (parse-int (config :docker-http-timeout)) 15000)
        cm (get-conn-manager)]
    (try
      (execute-in-scope {:method        method
                         :url           (url api)
                         :options       (merge {:connection-manager          cm
                                                :connection-request-timeout  timeout-ms
                                                :socket-timeout              timeout-ms
                                                :connection-timeout          (max timeout-ms 5000)
                                                :retry-handler               (fn [& _] false)} options)
                         :scope         "Docker"
                         :timeout       (+ timeout-ms 5000)
                         :error-handler :message})
      (finally
        (.shutdown cm)))))
