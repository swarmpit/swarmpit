(ns swarmpit.couchdb.client
  (:require [swarmpit.http :refer :all]
            [cheshire.core :refer [generate-string]]
            [swarmpit.config :refer [config]]
            [clojure.string :as str]))

(defn- execute
  [{:keys [method api options]}]
  (let [url (str (config :db-url) api)]
    (execute-in-scope {:method  method
                       :url     url
                       :options options
                       :scope   "DB"})))

(defn get-doc
  [id]
  (when (not (str/blank? id))
    (try
      (-> (execute {:method :GET
                    :api    (str "/swarmpit/" id)})
          :body)
      (catch Exception _))))

(defn create-doc
  [doc]
  (-> (execute {:method  :POST
                :api     "/swarmpit"
                :options {:body    doc
                          :headers {:Accept       "application/json"
                                    :Content-Type "application/json"}}})
      :body))

(defn find-cross-docs
  [query]
  (-> (execute {:method  :POST
                :api     "/swarmpit/_find"
                :options {:body    {:selector query}
                          :headers {:Accept       "application/json"
                                    :Content-Type "application/json"}}})
      :body
      :docs))

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

(defn version
  []
  (-> (execute {:method :GET
                :api    "/"})
      :body))

(defn database-exist?
  []
  (-> (execute {:method :HEAD
                :api    "/swarmpit"})
      :status))

(defn create-database
  []
  (-> (execute {:method :PUT
                :api    "/swarmpit"})
      :body))

(defn create-sns-users
  []
  (-> (execute {:method :PUT
                :api    "/_users"})
      :body))

(defn create-sns-replicator
  []
  (-> (execute {:method :PUT
                :api    "/_replicator"})
      :body))

(defn create-sns-global-changes
  []
  (-> (execute {:method :PUT
                :api    "/_global_changes"})
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
  (-> (assoc secret :type "secret")
      (create-doc)))

(defn get-secret
  []
  (find-doc {} "secret"))

;; Registry Dockerhub

(defn dockerhubs
  [owner]
  (if (nil? owner)
    (find-docs "dockerhub")
    (find-docs {"$or" [{:owner {"$eq" owner}}
                       {:public {"$eq" true}}]} "dockerhub")))

(defn dockerhub
  ([id]
   (get-doc id))
  ([username owner]
   (find-doc {:username username
              :owner    owner} "dockerhub")))

(defn create-dockerhub
  [docker-user]
  (-> (assoc docker-user :type "dockerhub")
      (create-doc)))

(defn update-dockerhub
  [docker-user delta]
  (let [allowed-delta (dissoc delta :_id :_rev :username)]
    (update-doc docker-user allowed-delta)))

(defn delete-dockerhub
  [docker-user]
  (delete-doc docker-user))

;; Registry v2

(defn registries-v2
  [owner]
  (if (nil? owner)
    (find-docs "v2")
    (find-docs {"$or" [{:owner {"$eq" owner}}
                       {:public {"$eq" true}}]} "v2")))

(defn registry-v2
  ([id]
   (get-doc id))
  ([name owner]
   (find-doc {:name  name
              :owner owner} "v2")))

(defn create-v2-registry
  [registry]
  (-> (assoc registry :type "v2")
      (create-doc)))

(defn update-v2-registry
  [registry delta]
  (let [allowed-delta (dissoc delta :_id :_rev :name)]
    (update-doc registry allowed-delta)))

(defn delete-v2-registry
  [registry]
  (delete-doc registry))

;; Registry AWS ECR

(defn registries-ecr
  [owner]
  (if (nil? owner)
    (find-docs "ecr")
    (find-docs {"$or" [{:owner {"$eq" owner}}
                       {:public {"$eq" true}}]} "ecr")))

(defn registry-ecr
  ([id]
   (get-doc id))
  ([user owner]
   (find-doc {:user  user
              :owner owner} "ecr")))

(defn create-ecr-registry
  [ecr]
  (-> (assoc ecr :type "ecr")
      (create-doc)))

(defn update-ecr-registry
  [ecr delta]
  (let [allowed-delta (dissoc delta :_id :_rev)]
    (update-doc ecr allowed-delta)))

(defn delete-ecr-registry
  [ecr]
  (delete-doc ecr))

;; Registry Azure ACR

(defn registries-acr
  [owner]
  (if (nil? owner)
    (find-docs "acr")
    (find-docs {"$or" [{:owner {"$eq" owner}}
                       {:public {"$eq" true}}]} "acr")))

(defn registry-acr
  ([id]
   (get-doc id))
  ([user owner]
   (find-doc {:user  user
              :owner owner} "acr")))

(defn create-acr-registry
  [acr]
  (-> (assoc acr :type "acr")
      (create-doc)))

(defn update-acr-registry
  [acr delta]
  (let [allowed-delta (dissoc delta :_id :_rev)]
    (update-doc acr allowed-delta)))

(defn delete-acr-registry
  [acr]
  (delete-doc acr))

;; Registry Gitlab

(defn registries-gitlab
  [owner]
  (if (nil? owner)
    (find-docs "gitlab")
    (find-docs {"$or" [{:owner {"$eq" owner}}
                       {:public {"$eq" true}}]} "gitlab")))

(defn registry-gitlab
  ([id]
   (get-doc id))
  ([username owner]
   (find-doc {:username username
              :owner    owner} "gitlab")))

(defn create-gitlab-registry
  [registry]
  (-> (assoc registry :type "gitlab")
      (create-doc)))

(defn update-gitlab-registry
  [registry delta]
  (let [allowed-delta (dissoc delta :_id :_rev :name)]
    (update-doc registry allowed-delta)))

(defn delete-gitlab-registry
  [registry]
  (delete-doc registry))

;; User

(defn users
  []
  (find-docs "user"))

(defn user
  [id]
  (get-doc id))

(defn user-registries
  [username]
  (find-cross-docs
    {"$and" [{:type {"$in" ["dockerhub" "v2" "ecr" "acr" "gitlab"]}}
             {:owner {"$eq" username}}]}))

(defn user-by-username
  [username]
  (find-doc {:username {"$eq" username}} "user"))

(defn create-user
  [user]
  (-> (assoc user :type "user")
      (create-doc)))

(defn delete-user
  [user]
  (delete-doc user))

(defn delete-user-registries
  [username]
  (doseq [reg (user-registries username)]
    (delete-doc reg)))

(defn update-user
  [user delta]
  (let [allowed-delta (select-keys delta [:role :email])]
    (update-doc user allowed-delta)))

(defn change-password
  [user encrypted-password]
  (update-doc user :password encrypted-password))

(defn set-api-token
  [user api-token]
  (update-doc user :api-token api-token))

(defn update-dashboard
  [user dashboard-type dashboard]
  (update-doc user dashboard-type dashboard))

;; Stackfile

(defn stackfiles
  []
  (find-docs "stackfile"))

(defn stackfile
  [name]
  (find-doc {:name {"$eq" name}} "stackfile"))

(defn create-stackfile
  [stackfile]
  (-> (assoc stackfile :type "stackfile")
      (create-doc)))

(defn update-stackfile
  [stackfile delta]
  (let [allowed-delta (select-keys delta [:spec :previousSpec])]
    (update-doc stackfile allowed-delta)))

(defn delete-stackfile
  [stackfile]
  (delete-doc stackfile))