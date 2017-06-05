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

(defn- execute
  [call-fx]
  (let [{:keys [status body error]} call-fx]
    (if error
      (throw
        (ex-info "Clutch DB client failure!"
                 {:status 500
                  :body   {:error (:cause (Throwable->map error))}}))
      (let [response (parse-string body true)]
        (if (> 400 status)
          response
          (throw
            (ex-info "Clutch DB error!"
                     {:status status
                      :body   {:error response}})))))))

(defn- get
  [api]
  (let [url (str base-url api)
        options {:headers headers}]
    (execute @(http/get url options))))

(defn- put
  [api]
  (let [url (str base-url api)
        options {:headers headers}]
    (execute @(http/put url options))))

(defn- post
  [api request]
  (let [url (str base-url api)
        options {:headers headers
                 :body    (generate-string request)}]
    (execute @(http/post url options))))

;; Database

(defn create-database
  []
  (put "/swarmpit"))

;; Registry

(defn registries
  []
  (->> {:selector {:type {"$eq" "registry"}}}
       (post "/swarmpit/_find")
       :docs))

(defn registry-by-name
  [name]
  (->> {:selector {:type {"$eq" "registry"}
                   :name {"$eq" name}}}
       (post "/swarmpit/_find")
       :docs
       (first)))

(defn create-registry
  [registry]
  (post "/swarmpit" registry))

;; User

(defn users
  []
  (->> {:selector {:type {"$eq" "user"}}}
       (post "/swarmpit/_find")
       :docs))

(defn user-by-credentials
  [username password]
  (->> {:selector {:type     {"$eq" "user"}
                   :username {"$eq" username}
                   :password {"$eq" password}}}
       (post "/swarmpit/_find")
       :docs
       (first)))

(defn user-by-username
  [username]
  (->> {:selector {:type     {"$eq" "user"}
                   :username {"$eq" username}}}
       (post "/swarmpit/_find")
       :docs
       (first)))

(defn create-user
  [user]
  (post "/swarmpit" user))