(ns swarmpit.couchdb.client
  (:refer-clojure :exclude [get find])
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string generate-string]]
            [swarmpit.config :refer [config]]))

(def headers
  {"Accept"       "application/json"
   "Content-Type" "application/json"})

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
  (let [url (str (config :db-url) api)
        options {:headers headers}]
    (execute @(http/get url options))))

(defn- put
  ([api] (put api {}))
  ([api request] (let [url (str (config :db-url) api)
                       options {:headers headers
                                :body    (generate-string request)}]
                   (execute @(http/put url options)))))

(defn- post
  [api request]
  (let [url (str (config :db-url) api)
        options {:headers headers
                 :body    (generate-string request)}]
    (execute @(http/post url options))))

(defn- delete
  [api params]
  (let [url (str (config :db-url) api)
        options {:headers      headers
                 :query-params params}]
    (execute @(http/delete url options))))

(defn- get-doc
  [id]
  (get (str "/swarmpit/" id)))

(defn- create-doc
  [doc]
  (post "/swarmpit" doc))

(defn- find-doc
  [query type]
  (->> {:selector (merge query {:type {"$eq" type}})}
       (post "/swarmpit/_find")
       :docs
       (first)))

(defn- find-all-docs
  [type]
  (->> {:selector {:type {"$eq" type}}}
       (post "/swarmpit/_find")
       :docs))

(defn- delete-doc
  [doc]
  (let [url (str "/swarmpit/" (:_id doc))]
    (delete url {:rev (:_rev doc)})))

(defn- update-doc
  ([doc delta]
   (let [url (str "/swarmpit/" (:_id doc))]
     (put url (merge doc delta))))
  ([doc field value]
   (let [url (str "/swarmpit/" (:_id doc))]
     (put url (assoc doc field value)))))

;; Database

(defn db-version
  []
  (get "/"))

(defn create-database
  []
  (put "/swarmpit"))

;; Secret

(defn create-secret
  [secret]
  (create-doc secret))

(defn get-secret
  []
  (find-doc {} "secret"))

;; Docker user

(defn docker-users
  []
  (find-all-docs "dockeruser"))

(defn docker-user
  [id]
  (get-doc id))

(defn docker-user-by-name
  [username]
  (find-doc {:username {"$eq" username}} "dockeruser"))

(defn create-docker-user
  [docker-user]
  (create-doc docker-user))

(defn delete-docker-user
  [docker-user]
  (delete-doc docker-user))

;; Registry

(defn registries
  []
  (find-all-docs "registry"))

(defn registry
  [id]
  (get-doc id))

(defn registry-by-name
  [name]
  (find-doc {:name {"$eq" name}} "registry"))

(defn create-registry
  [registry]
  (create-doc registry))

(defn delete-registry
  [registry]
  (delete-doc registry))

;; User

(defn users
  []
  (find-all-docs "user"))

(defn user
  [id]
  (get-doc id))

(defn user-by-username
  [username]
  (find-doc {:username {"$eq" username}} "user"))

(defn create-user
  [user]
  (create-doc user))

(defn delete-user
  [user]
  (delete-doc user))

(defn update-user
  [user delta]
  (let [allowed-delta (select-keys delta [:role :email])]
    (update-doc user allowed-delta)))

(defn change-password
  [user encrypted-password]
  (update-doc user :password encrypted-password))