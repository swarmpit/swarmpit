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
  (let [{:keys [status body error]} call-fx]
    (if error
      (throw
        (ex-info "Registry client failure!"
                 {:status 500
                  :body   {:error (:cause (Throwable->map error))}}))
      (let [response (parse-string body true)]
        (if (> 400 status)
          response
          (throw
            (ex-info "Registry error!"
                     {:status status
                      :body   {:error (:errors response)}})))))))

(defn get
  [registry api]
  (let [url (str (:scheme registry) "://" (:url registry) "/" api-version api)
        options {:timeout 5000
                 :headers (headers (:user registry)
                                   (:password registry))}]
    (execute @(http/get url options))))
