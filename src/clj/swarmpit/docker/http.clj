(ns swarmpit.docker.http
  (:require [clj-http.conn-mgr :as conn-mgr]
            [swarmpit.http :refer :all]
            [cheshire.core :refer [parse-string generate-string]]
            [swarmpit.config :refer [config]])
  (:import (org.apache.http.config RegistryBuilder)
           (swarmpit.socket UnixSocketFactory)
           (org.apache.http.impl.conn BasicHttpClientConnectionManager)))

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

(def timeout 5000)

(defn execute
  "Execute docker command and parse result"
  [{:keys [method api options]}]
  (let [options (req-options options)]
    (execute-in-scope {:method        method
                       :url           (url api)
                       :options       (merge {:connection-manager (make-conn-manager)
                                              :retry-handler      (fn [& _] false)} options)
                       :scope         "Docker"
                       :timeout       timeout
                       :error-handler :message})))