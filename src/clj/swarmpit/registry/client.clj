(ns swarmpit.registry.client
  (:refer-clojure :exclude [get])
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string]]
            [swarmpit.token :as token]))

(def ^:private api-version "v2")

(defn headers
  [user password]
  (let [token (token/generate-basic user password)]
    {"Authorization" token}))

(defn execute
  [call-fx]
  (let [{:keys [body error]} call-fx]
    (if error
      (throw (ex-info "Failed connect to registry!" {:error error}))
      (parse-string body true))))

(defn get
  [url api headers]
  (let [url (str url "/" api-version api)
        options {:headers headers}]
    (execute @(http/get url options))))