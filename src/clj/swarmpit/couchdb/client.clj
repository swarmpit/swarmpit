(ns swarmpit.couchdb.client
  (:refer-clojure :exclude [get find])
  (:require [clj-http.client :as http]
            [swarmpit.http :refer :all]
            [cheshire.core :refer [generate-string]]
            [swarmpit.config :refer [config]]))

(def ^:private headers
  {"Accept"       "application/json"
   "Content-Type" "application/json"})

(defn- execute
  [call]
  (execute-in-scope {:call-fx call
                     :scope   "DB"}))

(defn- get
  [api]
  (let [url (str (config :db-url) api)
        options {:headers headers}]
    (execute #(http/get url options))))

(defn- put
  ([api] (put api {}))
  ([api request] (let [url (str (config :db-url) api)
                       options {:headers headers
                                :body    (generate-string request)}]
                   (execute #(http/put url options)))))

(defn- post
  [api request]
  (let [url (str (config :db-url) api)
        options {:headers headers
                 :body    (generate-string request)}]
    (execute #(http/post url options))))

(defn- delete
  [api params]
  (let [url (str (config :db-url) api)
        options {:headers      headers
                 :query-params params}]
    (execute #(http/delete url options))))

(defn get-doc
  [id]
  (try
    (if (empty? id)
      nil
      (get (str "/swarmpit/" id)))
    (catch Exception _)))

(defn create-doc
  [doc]
  (post "/swarmpit" doc))

(defn find-docs
  ([type]
   (find-docs nil type))
  ([query type]
   (->> {:selector (merge query {:type {"$eq" type}})}
        (post "/swarmpit/_find")
        :docs)))

(defn find-doc
  [query type]
  (first (find-docs query type)))

(defn delete-doc
  [doc]
  (let [url (str "/swarmpit/" (:_id doc))]
    (delete url {:rev (:_rev doc)})))

(defn update-doc
  ([doc]
   (let [url (str "/swarmpit/" (:_id doc))]
     (put url doc)))
  ([doc delta]
   (update-doc (merge doc delta)))
  ([doc field value]
   (update-doc (assoc doc field value))))

;; Database

(defn db-version
  []
  (get "/"))

(defn create-database
  []
  (put "/swarmpit"))

;; Migration
(defn migrations
  []
  (->> (find-docs "migration")
       (map :name)
       (map keyword)
       (set)))

(defn record-migration
  [name result]
  (create-doc {:type   "migration"
               :name   name
               :result result}))

;; Secret

(defn create-secret
  [secret]
  (create-doc secret))

(defn get-secret
  []
  (find-doc {} "secret"))

;; Docker user

(defn dockerusers
  [owner]
  (find-docs {"$or" [{:owner {"$eq" owner}}
                     {:public {"$eq" true}}]} "dockeruser"))

(defn dockeruser
  ([id]
   (get-doc id))
  ([username owner]
   (find-doc {:username username
              :owner    owner} "dockeruser")))

(defn dockeruser-exist?
  [docker-user]
  (some? (find-doc {:username {"$eq" (:username docker-user)}
                    :owner    {"$eq" (:owner docker-user)}} "dockeruser")))

(defn create-dockeruser
  [docker-user]
  (create-doc docker-user))

(defn update-dockeruser
  [docker-user delta]
  (let [allowed-delta (select-keys delta [:public])]
    (update-doc docker-user allowed-delta)))

(defn delete-dockeruser
  [docker-user]
  (delete-doc docker-user))

;; Registry

(defn registries
  [owner]
  (find-docs {"$or" [{:owner {"$eq" owner}}
                     {:public {"$eq" true}}]} "registry"))

(defn registry
  [id]
  (get-doc id))

(defn registry
  ([id]
   (get-doc id))
  ([name owner]
   (find-doc {:name  name
              :owner owner} "registry")))

(defn registry-exist?
  [registry]
  (some? (find-doc {:name  {"$eq" (:name registry)}
                    :owner {"$eq" (:owner registry)}} "registry")))

(defn create-registry
  [registry]
  (create-doc registry))

(defn update-registry
  [user delta]
  (let [allowed-delta (select-keys delta [:public])]
    (update-doc user allowed-delta)))

(defn delete-registry
  [registry]
  (delete-doc registry))

;; User

(defn users
  []
  (find-docs "user"))

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