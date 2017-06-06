(ns swarmpit.couchdb.client
  (:refer-clojure :exclude [get find])
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

(defn- find
  [query type]
  (->> {:selector (merge query {:type {"$eq" type}})}
       (post "/swarmpit/_find")
       :docs
       (first)))

(defn- find-all
  [type]
  (->> {:selector {:type {"$eq" type}}}
       (post "/swarmpit/_find")
       :docs))

;; Database

(defn create-database
  []
  (put "/swarmpit"))

;; Registry

(defn registries
  []
  (find-all "registry"))

(defn registry
  [id]
  (find {:id {"$eq" id}} "registry"))

(defn registry-by-name
  [name]
  (find {:name {"$eq" name}} "registry"))

(defn create-registry
  [registry]
  (post "/swarmpit" registry))

;; User

(defn users
  []
  (find-all "user"))

(defn user
  [id]
  (find {:id {"$eq" id}} "user"))

(defn user-by-credentials
  [username password]
  (find {:username {"$eq" username}
         :password {"$eq" password}} "user"))

(defn user-by-username
  [username]
  (find {:username {"$eq" username}} "user"))

(defn create-user
  [user]
  (post "/swarmpit" user))