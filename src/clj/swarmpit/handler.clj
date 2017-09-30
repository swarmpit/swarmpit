(ns swarmpit.handler
  (:require [clojure.walk :refer [keywordize-keys]]
            [clojure.java.io :as io]
            [swarmpit.version :refer [version]]
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

;; Version handler

(defmethod dispatch :version [_]
  (fn [_]
    (resp-ok version)))

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
  (fn [{:keys [params identity]}]
    (let [username (get-in identity [:usr :username])
          payload (keywordize-keys params)]
      (if (api/user-by-credentials (merge payload {:username username}))
        (do (-> (api/user-by-username username)
                (api/change-password (:new-password payload)))
            (resp-ok))
        (resp-error 403 "Invalid old password provided")))))

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
  (fn [{:keys [route-params identity]}]
    (let [user (get-in identity [:usr :username])]
      (if (= (:_id (api/user-by-username user))
             (:id route-params))
        (resp-error 400 "Operation not allowed")
        (do (api/delete-user (:id route-params))
            (resp-ok))))))

(defmethod dispatch :user-create [_]
  (fn [{:keys [params]}]
    (let [payload (assoc (keywordize-keys params) :type "user")
          response (api/create-user payload)]
      (if (some? response)
        (resp-created (select-keys response [:id]))
        (resp-error 400 "User already exist")))))

(defmethod dispatch :user-update [_]
  (fn [{:keys [route-params params]}]
    (let [payload (keywordize-keys params)]
      (api/update-user (:id route-params) payload)
      (resp-ok))))

;; Service handler

(defmethod dispatch :services [_]
  (fn [_]
    (->> (api/services)
         (resp-ok))))

(defmethod dispatch :service [_]
  (fn [{:keys [route-params]}]
    (->> (api/service (:id route-params))
         (resp-ok))))

(defmethod dispatch :service-networks [_]
  (fn [{:keys [route-params]}]
    (->> (api/service-networks (:id route-params))
         (resp-ok))))

(defmethod dispatch :service-tasks [_]
  (fn [{:keys [route-params]}]
    (->> (api/service-tasks (:id route-params))
         (resp-ok))))

(defmethod dispatch :service-logs [_]
  (fn [{:keys [route-params query-params]}]
    (->> (keywordize-keys query-params)
         :from
         (api/service-logs (:id route-params))
         (resp-ok))))

(defmethod dispatch :service-create [_]
  (fn [{:keys [params]}]
    (let [payload (keywordize-keys params)]
      (->> (api/create-service payload)
           (resp-created)))))

(defmethod dispatch :service-update [_]
  (fn [{:keys [route-params params]}]
    (let [payload (keywordize-keys params)]
      (api/update-service (:id route-params) payload false)
      (resp-ok))))

(defmethod dispatch :service-redeploy [_]
  (fn [{:keys [route-params]}]
    (api/redeploy-service (:id route-params))
    (resp-ok)))

(defmethod dispatch :service-rollback [_]
  (fn [{:keys [route-params]}]
    (api/rollback-service (:id route-params))
    (resp-ok)))

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

;; Placement handler

(defmethod dispatch :placement [_]
  (fn [_]
    (->> (api/placement)
         (resp-ok))))

;; Plugin handler

(defmethod dispatch :plugin-network [_]
  (fn [_]
    (->> (api/plugins-by-type "Network")
         (filter #(not (contains? #{"null" "host"} %)))
         (resp-ok))))

(defmethod dispatch :plugin-log [_]
  (fn [_]
    (->> (api/plugins-by-type "Log")
         (resp-ok))))

(defmethod dispatch :plugin-volume [_]
  (fn [_]
    (->> (api/plugins-by-type "Volume")
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

;; Registry handler

(defmethod dispatch :registries [_]
  (fn [{:keys [identity]}]
    (let [owner (get-in identity [:usr :username])]
      (->> (api/registries owner)
           (resp-ok)))))

(defmethod dispatch :registry [_]
  (fn [{:keys [route-params]}]
    (->> (api/registry (:id route-params))
         (resp-ok))))

(defmethod dispatch :registry-delete [_]
  (fn [{:keys [route-params]}]
    (api/delete-registry (:id route-params))
    (resp-ok)))

(defmethod dispatch :registry-create [_]
  (fn [{:keys [params identity]}]
    (let [owner (get-in identity [:usr :username])
          payload (assoc (keywordize-keys params) :owner owner
                                                  :type "registry")]
      (if (api/registry-valid? payload)
        (let [response (api/create-registry payload)]
          (if (some? response)
            (resp-created (select-keys response [:id]))
            (resp-error 400 "Registry already exist")))
        (resp-error 400 "Registry credentials does not match any known registry")))))

(defmethod dispatch :registry-update [_]
  (fn [{:keys [route-params params]}]
    (let [payload (keywordize-keys params)]
      (api/update-registry (:id route-params) payload)
      (resp-ok))))

(defmethod dispatch :registry-repositories [_]
  (fn [{:keys [route-params]}]
    (let [registry-id (:id route-params)]
      (->> (api/registry-repositories registry-id)
           (resp-ok)))))

(defmethod dispatch :registry-repository-tags [_]
  (fn [{:keys [route-params query-params]}]
    (let [query-params (keywordize-keys query-params)
          repository-name (:repository query-params)
          registry-id (:id route-params)]
      (if (nil? repository-name)
        (resp-error 400 "Parameter repository missing")
        (->> (api/registry-tags registry-id repository-name)
             (resp-ok))))))

(defmethod dispatch :registry-repository-ports [_]
  (fn [{:keys [route-params query-params]}]
    (let [query-params (keywordize-keys query-params)
          repository-name (:repositoryName query-params)
          repository-tag (:repositoryTag query-params)
          registry-id (:id route-params)]
      (if (or (nil? repository-name)
              (nil? repository-tag))
        (resp-error 400 "Parameter repositoryName or repositoryTag missing")
        (->> (api/registry-ports registry-id repository-name repository-tag)
             (resp-ok))))))

;; Dockerhub handler

(defmethod dispatch :dockerhub-users [_]
  (fn [{:keys [identity]}]
    (let [owner (get-in identity [:usr :username])]
      (->> (api/dockerusers owner)
           (resp-ok)))))

(defmethod dispatch :dockerhub-user [_]
  (fn [{:keys [route-params]}]
    (->> (api/dockeruser (:id route-params))
         (resp-ok))))

(defmethod dispatch :dockerhub-user-create [_]
  (fn [{:keys [params identity]}]
    (let [owner (get-in identity [:usr :username])
          payload (assoc (keywordize-keys params) :owner owner
                                                  :type "dockeruser")
          dockeruser-info (api/dockeruser-info payload)]
      (api/dockeruser-login payload)
      (let [response (api/create-dockeruser payload dockeruser-info)]
        (if (some? response)
          (resp-created (select-keys response [:id]))
          (resp-error 400 "Docker user already exist"))))))

(defmethod dispatch :dockerhub-user-update [_]
  (fn [{:keys [route-params params]}]
    (let [payload (keywordize-keys params)]
      (api/update-dockeruser (:id route-params) payload)
      (resp-ok))))

(defmethod dispatch :dockerhub-user-delete [_]
  (fn [{:keys [route-params]}]
    (api/delete-dockeruser (:id route-params))
    (resp-ok)))

(defmethod dispatch :dockerhub-repositories [_]
  (fn [{:keys [route-params]}]
    (let [dockeruser-id (:id route-params)]
      (->> (api/dockeruser-repositories dockeruser-id)
           (resp-ok)))))

(defmethod dispatch :dockerhub-repository-tags [_]
  (fn [{:keys [route-params query-params]}]
    (let [query-params (keywordize-keys query-params)
          repository-name (:repository query-params)
          dockeruser-id (:id route-params)]
      (if (nil? repository-name)
        (resp-error 400 "Parameter repository missing")
        (->> (api/dockeruser-tags dockeruser-id repository-name)
             (resp-ok))))))

(defmethod dispatch :dockerhub-repository-ports [_]
  (fn [{:keys [route-params query-params]}]
    (let [query-params (keywordize-keys query-params)
          repository-name (:repositoryName query-params)
          repository-tag (:repositoryTag query-params)
          dockeruser-id (:id route-params)]
      (if (or (nil? repository-name)
              (nil? repository-tag))
        (resp-error 400 "Parameter repositoryName or repositoryTag missing")
        (->> (api/dockeruser-ports dockeruser-id repository-name repository-tag)
             (resp-ok))))))

;; Public dockerhub handler

(defmethod dispatch :public-repositories [_]
  (fn [{:keys [query-params]}]
    (let [query-params (keywordize-keys query-params)
          repository-query (:query query-params)
          repository-page (:page query-params)]
      (->> (api/public-repositories repository-query repository-page)
           (resp-ok)))))

(defmethod dispatch :public-repository-tags [_]
  (fn [{:keys [query-params]}]
    (let [query-params (keywordize-keys query-params)
          repository-name (:repository query-params)]
      (if (nil? repository-name)
        (resp-error 400 "Parameter repository missing")
        (->> (api/public-tags repository-name)
             (resp-ok))))))

(defmethod dispatch :public-repository-ports [_]
  (fn [{:keys [query-params]}]
    (let [query-params (keywordize-keys query-params)
          repository-name (:repositoryName query-params)
          repository-tag (:repositoryTag query-params)]
      (if (or (nil? repository-name)
              (nil? repository-tag))
        (resp-error 400 "Parameter repositoryName or repositoryTag missing")
        (->> (api/public-ports repository-name repository-tag)
             (resp-ok))))))