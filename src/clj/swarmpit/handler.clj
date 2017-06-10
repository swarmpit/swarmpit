(ns swarmpit.handler
  (:require [cemerick.url :refer [query->map]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.java.io :as io]
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

;;; Handler

(defmulti dispatch identity)

;; Index handler

(defmethod dispatch :index [_]
  (fn [_]
    {:status  200
     :headers {"Content-Type" "text/html"}
     :body    (slurp (io/resource "public/index.html"))}))

;; Login handler

(defmethod dispatch :login [_]
  (fn [{:keys [headers]}]
    (let [token (get headers "authorization")]
      (if (nil? token)
        (resp-error 400 "Missing token")
        (let [user (->> (token/decode-basic token)
                        (api/user-by-credentials))]
          (if (nil? user)
            (resp-unauthorized "Invalid credentials")
            (resp-ok {:token (token/generate-jwt user)})))))))

;; User handler

(defmethod dispatch :users [_]
  (fn [_]
    (->> (api/users)
         (resp-ok))))

(defmethod dispatch :user [_]
  (fn [{:keys [route-params]}]
    (->> (api/user (:id route-params))
         (resp-ok))))

(defmethod dispatch :user-create [_]
  (fn [{:keys [params]}]
    (let [payload (keywordize-keys params)]
      (if (some? (api/create-registry payload))
        (resp-created)
        (resp-error 400 "User already exist")))))

;; Registry handler

(defmethod dispatch :registries [_]
  (fn [_]
    (->> (api/registries)
         (resp-ok))))

(defmethod dispatch :registry [_]
  (fn [{:keys [route-params]}]
    (->> (api/registry (:id route-params))
         (resp-ok))))

(defmethod dispatch :registries-sum [_]
  (fn [_]
    (->> (api/registries-sum)
         (resp-ok))))

(defmethod dispatch :registry-create [_]
  (fn [{:keys [params]}]
    (let [payload (keywordize-keys params)]
      (if (api/registry-valid? payload)
        (if (some? (api/create-registry payload))
          (resp-created)
          (resp-error 400 "Registry already exist"))
        (resp-error 400 "Registry credentials does not match any known registry")))))

;; Service handler

(defmethod dispatch :services [_]
  (fn [_]
    (->> (api/services)
         (resp-ok))))

(defmethod dispatch :service [_]
  (fn [{:keys [route-params]}]
    (->> (api/service (:id route-params))
         (resp-ok))))

(defmethod dispatch :service-create [_]
  (fn [{:keys [params]}]
    (let [payload (keywordize-keys params)]
      (->> (api/create-service payload)
           (resp-created)))))

(defmethod dispatch :service-update [_]
  (fn [{:keys [route-params params]}]
    (let [payload (keywordize-keys params)]
      (api/update-service (:id route-params) payload)
      (resp-ok))))

(defmethod dispatch :service-delete [_]
  (fn [{:keys [route-params]}]
    (api/delete-service (:id route-params))
    (resp-ok)))

;; Network handler

(defmethod dispatch :networks [_]
  (fn [_]
    (->> (api/networks)
         (resp-ok))))

(defmethod dispatch :network [_]
  (fn [{:keys [route-params]}]
    (->> (api/network (:id route-params))
         (resp-ok))))

(defmethod dispatch :network-create [_]
  (fn [{:keys [params]}]
    (let [payload (keywordize-keys params)]
      (->> (api/create-network payload)
           (resp-created)))))

(defmethod dispatch :network-delete [_]
  (fn [{:keys [route-params]}]
    (api/delete-network (:id route-params))
    (resp-ok)))

;; Node handler

(defmethod dispatch :nodes [_]
  (fn [_]
    (->> (api/nodes)
         (resp-ok))))

(defmethod dispatch :node [_]
  (fn [{:keys [route-params]}]
    (->> (api/node (:id route-params))
         (resp-ok))))

;; Task handler

(defmethod dispatch :tasks [_]
  (fn [_]
    (->> (api/tasks)
         (resp-ok))))

(defmethod dispatch :task [_]
  (fn [{:keys [route-params]}]
    (->> (api/task (:id route-params))
         (resp-ok))))

;; Repository handler

(defmethod dispatch :repositories [_]
  (fn [{:keys [route-params query-string]}]
    (let [query (keywordize-keys (query->map query-string))
          registry-name (:registryName route-params)
          repository-query (:repositoryQuery query)]
      (resp-ok
        (if (= "dockerhub" registry-name)
          (api/dockerhub-repositories repository-query (:repositoryPage query))
          (api/repositories registry-name repository-query))))))

(defmethod dispatch :repository-tags [_]
  (fn [{:keys [route-params query-string]}]
    (let [query (keywordize-keys (query->map query-string))
          repository-name (:repositoryName query)
          registry-name (:registryName route-params)]
      (if (nil? repository-name)
        (resp-error 400 "Parameter repositoryName missing")
        (resp-ok
          (if (= "dockerhub" registry-name)
            (api/dockerhub-tags repository-name)
            (api/tags registry-name repository-name)))))))