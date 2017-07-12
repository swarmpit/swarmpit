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

;; Password handler

(defmethod dispatch :password [_]
  (fn [{:keys [headers params]}]
    (let [token (get headers "authorization")
          username (token/user token)
          payload (keywordize-keys params)]
      (-> (api/user-by-username username)
          (api/change-password (:password payload))))
    (resp-ok)))

;; User handler

(defmethod dispatch :users [_]
  (fn [_]
    (->> (api/users)
         (resp-ok))))

(defmethod dispatch :user [_]
  (fn [{:keys [route-params]}]
    (->> (api/user (:id route-params))
         (resp-ok))))

(defmethod dispatch :user-delete [_]
  (fn [{:keys [headers route-params]}]
    (let [token (get headers "authorization")
          username (token/user token)
          user (api/user-by-username username)]
      (if (= (:_id user)
             (:id route-params))
        (resp-error 400 "Operation not allowed")
        (do (api/delete-user (:id route-params))
            (resp-ok))))))

(defmethod dispatch :user-create [_]
  (fn [{:keys [params]}]
    (let [payload (keywordize-keys params)
          response (api/create-user payload)]
      (if (some? response)
        (resp-created (select-keys response [:id]))
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

(defmethod dispatch :registry-delete [_]
  (fn [{:keys [route-params]}]
    (api/delete-registry (:id route-params))
    (resp-ok)))

(defmethod dispatch :registry-create [_]
  (fn [{:keys [params]}]
    (let [payload (keywordize-keys params)]
      (if (api/registry-valid? payload)
        (let [response (api/create-registry payload)]
          (if (some? response)
            (resp-created (select-keys response [:id]))
            (resp-error 400 "Registry already exist")))
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

;; Volume handler

(defmethod dispatch :volumes [_]
  (fn [_]
    (->> (api/volumes)
         (resp-ok))))

(defmethod dispatch :volume [_]
  (fn [{:keys [route-params]}]
    (->> (api/volume (:name route-params))
         (resp-ok))))

(defmethod dispatch :volume-create [_]
  (fn [{:keys [params]}]
    (let [payload (keywordize-keys params)]
      (->> (api/create-volume payload)
           (resp-created)))))

(defmethod dispatch :volume-delete [_]
  (fn [{:keys [route-params]}]
    (api/delete-volume (:name route-params))
    (resp-ok)))

;; Secret handler

(defmethod dispatch :secrets [_]
  (fn [_]
    (->> (api/secrets)
         (resp-ok))))

(defmethod dispatch :secret [_]
  (fn [{:keys [route-params]}]
    (->> (api/secret (:id route-params))
         (resp-ok))))

(defmethod dispatch :secret-create [_]
  (fn [{:keys [params]}]
    (let [payload (keywordize-keys params)]
      (->> (api/create-secret payload)
           (resp-created)))))

(defmethod dispatch :secret-delete [_]
  (fn [{:keys [route-params]}]
    (api/delete-secret (:id route-params))
    (resp-ok)))

(defmethod dispatch :secret-update [_]
  (fn [{:keys [route-params params]}]
    (let [payload (keywordize-keys params)]
      (api/update-secret (:id route-params) payload)
      (resp-ok))))

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

;; Registry v2 handler

(defmethod dispatch :repositories [_]
  (fn [{:keys [route-params]}]
    (let [registry-name (:registry route-params)
          registry (api/registry-by-name registry-name)]
      (if (nil? registry)
        (resp-error 400 "Unknown registry")
        (->> (api/repositories registry)
             (resp-ok))))))

(defmethod dispatch :repository-tags [_]
  (fn [{:keys [route-params query-string]}]
    (let [query (keywordize-keys (query->map query-string))
          repository-name (:repository query)
          registry-name (:registry route-params)
          registry (api/registry-by-name registry-name)]
      (if (nil? registry)
        (resp-error 400 "Unknown registry")
        (if (nil? repository-name)
          (resp-error 400 "Parameter repository missing")
          (->> (api/tags registry repository-name)
               (resp-ok)))))))

;; Dockerhub handler

(defmethod dispatch :dockerhub-users-sum [_]
  (fn [_]
    (->> (api/dockerusers-sum)
         (resp-ok))))

(defmethod dispatch :dockerhub-repo [_]
  (fn [{:keys [query-string]}]
    (let [query (keywordize-keys (query->map query-string))
          repository-query (:query query)
          repository-page (:page query)]
      (->> (api/dockerhub-repositories repository-query repository-page)
           (resp-ok)))))

(defmethod dispatch :dockerhub-tags [_]
  (fn [{:keys [query-string]}]
    (let [query (keywordize-keys (query->map query-string))
          repository-name (:repository query)
          dockeruser-name (:user query)]
      (if (nil? repository-name)
        (resp-error 400 "Parameter repository missing")
        (->> (api/dockerhub-tags repository-name dockeruser-name)
             (resp-ok))))))

(defmethod dispatch :dockerhub-user-repo [_]
  (fn [{:keys [route-params]}]
    (let [dockeruser (api/dockeruser-by-username (:user route-params))]
      (if (nil? dockeruser)
        (resp-error 400 "Unknown dockerhub user")
        (->> (api/dockeruser-repositories dockeruser)
             (resp-ok))))))

(defmethod dispatch :dockerhub-users [_]
  (fn [_]
    (->> (api/dockerusers)
         (resp-ok))))

(defmethod dispatch :dockerhub-user [_]
  (fn [{:keys [route-params]}]
    (->> (api/dockeruser (:id route-params))
         (resp-ok))))

(defmethod dispatch :dockerhub-user-create [_]
  (fn [{:keys [params]}]
    (let [payload (keywordize-keys params)
          user-info (api/dockeruser-info payload)]
      (api/dockeruser-login payload)
      (let [response (api/create-dockeruser payload user-info)]
        (if (some? response)
          (resp-created (select-keys response [:id]))
          (resp-error 400 "Docker user already exist"))))))

(defmethod dispatch :dockerhub-user-delete [_]
  (fn [{:keys [route-params]}]
    (api/delete-dockeruser (:id route-params))
    (resp-ok)))