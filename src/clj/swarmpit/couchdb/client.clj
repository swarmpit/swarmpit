(ns swarmpit.couchdb.client
  (:require [swarmpit.http :refer :all]
            [cheshire.core :refer [generate-string]]
            [swarmpit.config :refer [config]]))

(defn- execute
  [{:keys [method api options]}]
  (let [url (str (config :db-url) api)]
    (execute-in-scope {:method  method
                       :url     url
                       :options options
                       :scope   "DB"})))

(defn get-doc
  [id]
  (try
    (-> (execute {:method :GET
                  :api    (str "/swarmpit/" id)})
        :body)
    (catch Exception _)))

(defn create-doc
  [doc]
  (-> (execute {:method  :POST
                :api     "/swarmpit"
                :options {:body    doc
                          :headers {:Accept       "application/json"
                                    :Content-Type "application/json"}}})
      :body))

(defn find-docs
  ([type]
   (find-docs nil type))
  ([query type]
   (-> (execute {:method  :POST
                 :api     "/swarmpit/_find"
                 :options {:body    {:selector (merge query {:type {"$eq" type}})}
                           :headers {:Accept       "application/json"
                                     :Content-Type "application/json"}}})
       :body
       :docs)))

(defn find-doc
  [query type]
  (first (find-docs query type)))

(defn delete-doc
  [doc]
  (-> (execute {:method  :DELETE
                :api     (str "/swarmpit/" (:_id doc))
                :options {:query-params {:rev (:_rev doc)}}})
      :body))

(defn update-doc
  ([doc]
   (-> (execute {:method  :PUT
                 :api     (str "/swarmpit/" (:_id doc))
                 :options {:body    doc
                           :headers {:Accept       "application/json"
                                     :Content-Type "application/json"}}})
       :body))
  ([doc delta]
   (update-doc (merge doc delta)))
  ([doc field value]
   (update-doc (assoc doc field value))))

;; Database

(defn db-version
  []
  (-> (execute {:method :GET
                :api    "/"})
      :body))

(defn create-database
  []
  (-> (execute {:method :PUT
                :api    "/swarmpit"})
      :body))

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