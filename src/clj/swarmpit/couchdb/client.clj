(ns swarmpit.couchdb.client
  (:refer-clojure :exclude [get])
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string generate-string]]))

(def ^:private base-domain "localhost")
(def ^:private base-port 5984)
(def ^:private base-url
  (str "http://" base-domain ":" base-port))

(def headers
  {"Accept"       "application/json"
   "Content-Type" "application/json"
   "Host"         "localhost:5984"})

(defn execute
  [call-fx]
  (let [{:keys [body error]} call-fx]
    (if error
      (throw (ex-info "Failed connect to clutchdb!" {:error error}))
      (parse-string body true))))

(defn get
  [api]
  (let [options {:headers headers}]
    (execute @(http/get api options))))

(defn put
  [api]
  (let [url (str base-url api)
        options {:headers headers}]
    (execute @(http/put url options))))

(defn post
  [api request]
  (let [url (str base-url api)
        options {:headers headers
                 :body    (generate-string request)}]
    (execute @(http/post url options))))