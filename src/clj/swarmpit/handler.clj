(ns swarmpit.handler
  (:require [cemerick.url :refer [query->map]]
            [clojure.walk :refer [keywordize-keys]]
            [swarmpit.api :as api]
            [swarmpit.token :as token]))

(defn resp-error
  [status response]
  {:status status
   :body   {:error response}})

(defn resp-unauthorized
  [response]
  {:status 401
   :body   {:error response}})

(defn resp-ok
  ([] {:status 200})
  ([response] {:status 200
               :body   response}))

(defn resp-created
  ([] {:status 201})
  ([response] {:status 201
               :body   response}))

;;; Login handler

(defn login
  [{:keys [headers]}]
  (let [token (get headers "authorization")]
    (if (nil? token)
      (resp-error 400 "Missing token")
      (let [user (->> (token/decode-basic token)
                      (api/user-by-credentials))]
        (if (nil? user)
          (resp-unauthorized "Invalid credentials")
          (resp-ok {:token (token/generate-jwt user)}))))))

;;; User handler

(defn users
  [_]
  (->> (api/users)
       (resp-ok)))

(defn user-create
  [{:keys [params]}]
  (let [payload (keywordize-keys params)]
    (if (some? (api/create-registry payload))
      (resp-created)
      (resp-error 400 "User already exist"))))

;;; Registry handler

(defn registries
  [_]
  (->> (api/registries)
       (resp-ok)))

(defn registry
  [{:keys [route-params]}]
  (->> (api/registry (:id route-params))
       (resp-ok)))

(defn registries-sum
  [_]
  (->> (api/registries-sum)
       (resp-ok)))

(defn registry-create
  [{:keys [params]}]
  (let [payload (keywordize-keys params)]
    (if (api/registry-valid? payload)
      (if (some? (api/create-registry payload))
        (resp-created)
        (resp-error 400 "Registry already exist"))
      (resp-error 400 "Registry credentials does not match any known registry"))))

;;; Service handler

(defn services
  [_]
  (->> (api/services)
       (resp-ok)))

(defn service
  [{:keys [route-params]}]
  (->> (api/service (:id route-params))
       (resp-ok)))

(defn service-create
  [{:keys [params]}]
  (let [payload (keywordize-keys params)]
    (->> (api/create-service payload)
         (resp-created))))

(defn service-update
  [{:keys [route-params params]}]
  (let [payload (keywordize-keys params)]
    (api/update-service (:id route-params) payload)
    (resp-ok)))

(defn service-delete
  [{:keys [route-params]}]
  (api/delete-service (:id route-params))
  (resp-ok))

;;; Network handler

(defn networks
  [_]
  (->> (api/networks)
       (resp-ok)))

(defn network
  [{:keys [route-params]}]
  (->> (api/network (:id route-params))
       (resp-ok)))

(defn network-create
  [{:keys [params]}]
  (let [payload (keywordize-keys params)]
    (->> (api/create-network payload)
         (resp-created))))

(defn network-delete
  [{:keys [route-params]}]
  (api/delete-network (:id route-params))
  (resp-ok))

;;; Node handler

(defn nodes
  [_]
  (->> (api/nodes)
       (resp-ok)))

(defn node
  [{:keys [route-params]}]
  (->> (api/node (:id route-params))
       (resp-ok)))

;;; Task handler

(defn tasks
  [_]
  (->> (api/tasks)
       (resp-ok)))

(defn task
  [{:keys [route-params]}]
  (->> (api/task (:id route-params))
       (resp-ok)))

;;; Repository handler

(defn v1-repositories
  [{:keys [route-params query-string]}]
  (let [query (keywordize-keys (query->map query-string))]
    (->> (api/v1-repositories (:registryName route-params)
                              (:repositoryQuery query)
                              (:repositoryPage query))
         (resp-ok))))

(defn v2-repositories
  [{:keys [route-params query-string]}]
  (let [query (keywordize-keys (query->map query-string))]
    (->> (api/v2-repositories (:registryName route-params)
                              (:repositoryQuery query))
         (resp-ok))))

(defn v1-repository-tags
  [{:keys [route-params query-string]}]
  (let [query (keywordize-keys (query->map query-string))
        repository (:repositoryName query)]
    (if (nil? repository)
      (resp-error 400 "Parameter repositoryName missing")
      (->> (api/v1-tags (:registryName route-params)
                        repository)
           (resp-ok)))))

(defn v2-repository-tags
  [{:keys [route-params query-string]}]
  (let [query (keywordize-keys (query->map query-string))
        repository (:repositoryName query)]
    (if (nil? repository)
      (resp-error 400 "Parameter repositoryName missing")
      (->> (api/v2-tags (:registryName route-params)
                        repository)
           (resp-ok)))))