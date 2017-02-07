(ns swarmpit.registry.client
  (:refer-clojure :exclude [get])
  (:import java.util.Base64)
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string]]))

(defn- basic-header [user password]
  (let [credentials (str user ":" password)
        credentials-bytes (.getBytes credentials)
        credentials-encoded (.encodeToString (Base64/getEncoder) credentials-bytes)]
    (str "Basic " credentials-encoded)))

(defn- headers
  [user password]
  (let [authentication (basic-header user password)]
    {"Authorization" authentication}))

(defn execute
  [call-fx]
  (let [{:keys [body error]} call-fx]
    (if error
      (println "Failed to connect to registry. Reason: %s" error)
      (parse-string body true))))

(defn get
  [host api headers]
  (let [url (str host api)
        options {:headers headers}]
    (execute @(http/get url options))))