(ns swarmpit.handler
  (:require [cemerick.url :refer [query->map]]
            [clojure.walk :refer [keywordize-keys]]
            [swarmpit.api :as api]
            [swarmpit.token :as token]))

(defn json-error
  [status response]
  {:status  status
   :headers {"Content-Type" "application/json"}
   :body    {:error response}})

(defn json-ok
  ([] {:status 200})
  ([response] {:status 200
               :body   response})
  ([status response] {:status status
                      :body   response}))

;;; Login handler

(defn login
  [{:keys [headers]}]
  (let [token (get headers "authorization")]
    (if (nil? token)
      (json-error 400 "Missing token")
      (let [user (->> (token/decode-basic token)
                      (api/user-by-credentials))]
        (if (nil? user)
          (json-error 401 "Invalid credentials")
          (json-ok 200 {:token (token/generate-jwt user)}))))))

;;; User handler

(defn users
  [_]
  (->> (api/users)
       (json-ok)))

(defn user-create
  [{:keys [params]}]
  (let [payload (keywordize-keys params)]
    (if (some? (api/create-registry payload))
      (json-ok 201)
      (json-error 400 "User already exist"))))

;;; Registry handler

(defn registries
  [_]
  (->> (api/registries)
       (json-ok)))

(defn registries-sum
  [_]
  (->> (api/registries-sum)
       (json-ok)))

(defn registry-create
  [{:keys [params]}]
  (let [payload (keywordize-keys params)]
    (if (api/valid-registry? payload)
      (if (some? (api/create-registry payload))
        (json-ok 201)
        (json-error 400 "Registry already exist"))
      (json-error 400 "Registry credentials does not match any known registry"))))

;;; Service handler

(defn services
  [_]
  (->> (api/services)
       (json-ok)))

(defn service
  [{:keys [route-params]}]
  (->> (api/service (:id route-params))
       (json-ok)))

(defn service-create
  [{:keys [params]}]
  (let [payload (keywordize-keys params)]
    (->> (api/create-service payload)
         (json-ok 201))))

(defn service-update
  [{:keys [route-params params]}]
  (let [payload (keywordize-keys params)]
    (api/update-service (:id route-params) payload)
    (json-ok)))

(defn service-delete
  [{:keys [route-params]}]
  (api/delete-service (:id route-params))
  (json-ok))

;;; Network handler

(defn networks
  [_]
  (->> (api/networks)
       (json-ok)))

(defn network
  [{:keys [route-params]}]
  (->> (api/network (:id route-params))
       (json-ok)))

(defn network-create
  [{:keys [params]}]
  (let [payload (keywordize-keys params)]
    (->> (api/create-network payload)
         (json-ok 201))))

(defn network-delete
  [{:keys [route-params]}]
  (api/delete-network (:id route-params))
  (json-ok))

;;; Node handler

(defn nodes
  [_]
  (->> (api/nodes)
       (json-ok)))

(defn node
  [{:keys [route-params]}]
  (->> (api/node (:id route-params))
       (json-ok)))

;;; Task handler

(defn tasks
  [_]
  (->> (api/tasks)
       (json-ok)))

(defn task
  [{:keys [route-params]}]
  (->> (api/task (:id route-params))
       (json-ok)))

;;; Repository handler

(defn v1-repositories
  [{:keys [route-params query-string]}]
  (let [query (keywordize-keys (query->map query-string))]
    (->> (api/v1-repositories (:registryName route-params)
                              (:repositoryQuery query)
                              (:repositoryPage query))
         (json-ok))))

(defn v2-repositories
  [{:keys [route-params query-string]}]
  (let [query (keywordize-keys (query->map query-string))]
    (->> (api/v2-repositories (:registryName route-params)
                              (:repositoryQuery query))
         (json-ok))))

(defn v1-repository-tags
  [{:keys [route-params query-string]}]
  (let [query (keywordize-keys (query->map query-string))
        repository (:repositoryName query)]
    (if (nil? repository)
      (json-error 400 "Parameter repositoryName missing")
      (->> (api/v1-tags (:registryName route-params)
                        repository)
           (json-ok)))))

(defn v2-repository-tags
  [{:keys [route-params query-string]}]
  (let [query (keywordize-keys (query->map query-string))
        repository (:repositoryName query)]
    (if (nil? repository)
      (json-error 400 "Parameter repositoryName missing")
      (->> (api/v2-tags (:registryName route-params)
                        repository)
           (json-ok)))))