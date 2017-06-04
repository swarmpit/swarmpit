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
       (json-ok 200)))

;;; Registry handler

(defn registries
  [_]
  (->> (api/registries)
       (json-ok 200)))

(defn registries-sum
  [_]
  (->> (api/registries-sum)
       (json-ok 200)))

(defn registry-create
  [{:keys [params]}]
  (let [payload (keywordize-keys params)]
    (if (api/valid-registry? payload)
      (->> (api/create-registry payload)
           (json-ok 201))
      (json-error 400 "Invalid registry credentials"))))

;;; Service handler

(defn services
  [_]
  (->> (api/services)
       (json-ok 200)))

(defn service
  [{:keys [route-params]}]
  (->> (api/service (:id route-params))
       (json-ok 200)))

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
       (json-ok 200)))

(defn network
  [{:keys [route-params]}]
  (->> (api/network (:id route-params))
       (json-ok 200)))

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
       (json-ok 200)))

(defn node
  [{:keys [route-params]}]
  (->> (api/node (:id route-params))
       (json-ok 200)))

;;; Task handler

(defn tasks
  [_]
  (->> (api/tasks)
       (json-ok 200)))

(defn task
  [{:keys [route-params]}]
  (->> (api/task (:id route-params))
       (json-ok 200)))

;;; Repository handler

(defn v1-repositories
  [{:keys [route-params query-string]}]
  (let [query (keywordize-keys (query->map query-string))]
    (->> (api/v1-repositories (:registryName route-params)
                              (:repositoryQuery query)
                              (:repositoryPage query))
         (json-ok 200))))

(defn v2-repositories
  [{:keys [route-params query-string]}]
  (let [query (keywordize-keys (query->map query-string))]
    (->> (api/v2-repositories (:registryName route-params)
                              (:repositoryQuery query))
         (json-ok 200))))

(defn v1-repository-tags
  [{:keys [route-params query-string]}]
  (let [query (keywordize-keys (query->map query-string))
        repository (:repositoryName query)]
    (if (nil? repository)
      (json-error 400 "Param repositoryName missing")
      (->> (api/v1-tags (:registryName route-params)
                        repository)
           (json-ok 200)))))

(defn v2-repository-tags
  [{:keys [route-params query-string]}]
  (let [query (keywordize-keys (query->map query-string))
        repository (:repositoryName query)]
    (if (nil? repository)
      (json-error 400 "Param repositoryName missing")
      (->> (api/v2-tags (:registryName route-params)
                        repository)
           (json-ok 200)))))