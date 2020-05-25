(ns swarmpit.docker.engine.http
  (:require [clj-http.conn-mgr :as conn-mgr]
            [swarmpit.http :refer :all]
            [cheshire.core :refer [parse-string generate-string]]
            [swarmpit.config :refer [config]]
            [swarmpit.utils :refer [parse-int]])
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

(defn- make-unix-conn-manager
  []
  (BasicHttpClientConnectionManager. (unix-scheme)))

(defn- make-conn-manager
  []
  (if (http?)
    (conn-mgr/make-regular-conn-manager {})
    (make-unix-conn-manager)))

(defn- url
  [uri]
  (let [server (if (http?)
                 (config :docker-sock)
                 "http://localhost")
        api (str "/v" (config :docker-api))]
    (str server api uri)))

(defn execute
  [{:keys [method api options]}]
  (execute-in-scope {:method        method
                     :url           (url api)
                     :options       (merge {:connection-manager (make-conn-manager)
                                            :retry-handler      (fn [& _] false)} options)
                     :scope         "Docker"
                     :timeout       (parse-int (config :docker-http-timeout))
                     :error-handler :message}))