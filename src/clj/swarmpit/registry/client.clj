(ns swarmpit.registry.client
  (:refer-clojure :exclude [get])
  (:import java.util.Base64)
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string]]))

(def ^:private api-version "v2")
(def ^:private base-domain "swarmhub-csincz.azurecr.io")
(def ^:private base-url
  (str "https://" base-domain "/" api-version))

(defn- basic-header [user password]
  (let [credentials (str user ":" password)
        credentials-bytes (.getBytes credentials)
        credentials-encoded (.encodeToString (Base64/getEncoder) credentials-bytes)]
    (str "Basic " credentials-encoded)))

(defn headers
  [user password]
  (let [authentication (basic-header user password)]
    {"Authorization" authentication}))

(defn execute
  [call-fx]
  (let [{:keys [body error]} call-fx]
    (if error
      (throw (ex-info "Failed connect to registry!" {:error error}))
      (parse-string body true))))

(defn get
  [api headers]
  (let [url (str base-url api)
        options {:headers headers}]
    (execute @(http/get url options))))