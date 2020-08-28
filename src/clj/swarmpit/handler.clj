(ns swarmpit.handler
  (:require [clojure.walk :refer [keywordize-keys]]
            [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :as html :refer [deftemplate]]
            [swarmpit.api :as api]
            [swarmpit.slt :as slt]
            [swarmpit.token :as token]
            [swarmpit.stats :as stats]
            [swarmpit.version :as version]))

(defn include-css [href revision]
  (first (html/html [:link {:href (str href "?r=" revision) :rel "stylesheet"}])))

(defn include-js [src revision]
  (first (html/html [:script {:src (str src "?r=" revision)}])))

(deftemplate index-page "index.html"
             [revision]
             [:head] (html/append (map #(include-css % revision) ["css/main.css"]))
             [:body] (html/append (map #(include-js % revision) ["js/main.js"])))

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

(defn resp-accepted
  ([] {:status 202})
  ([response] {:status 202
               :body   response}))

(defn validate-parameters!
  [valid-premise error]
  (when-not valid-premise
    (throw
      (ex-info error
               {:status 400
                :type   :api
                :body   {:error error}}))))

;; Index handler

(defn index
  [_]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (apply str (index-page (:revision (version/info))))})

;; Version handler

(defn version
  [_]
  (->> (version/info)
       (resp-ok)))

;; SLT handler

(defn slt
  [{{:keys [usr]} :identity}]
  (resp-ok {:slt (slt/create (:username usr))}))

;; Login handler

(defn login
  [{:keys [headers]}]
  (let [token (get headers "authorization")]
    (if (nil? token)
      (resp-error 400 "Missing token")
      (let [user (->> (token/decode-basic token)
                      (api/user-by-credentials))]
        (if (nil? user)
          (resp-unauthorized "The username or password you entered is incorrect.")
          (resp-ok {:token (token/generate-jwt user)}))))))

;; Password handler

(defn password
  [{{:keys [body]} :parameters
    {:keys [usr]}  :identity}]
  (let [username (:username usr)]
    (if (api/user-by-credentials (merge body {:username username}))
      (do (-> (api/user-by-username username)
              (api/change-password (:new-password body)))
          (resp-ok))
      (resp-error 403 "Invalid old password provided"))))

;; User api token handler

(defn api-token-generate
  [{{:keys [usr]} :identity}]
  (->> (:username usr)
       (api/user-by-username)
       (api/generate-api-token)
       (resp-ok)))

(defn api-token-remove
  [{{:keys [usr]} :identity}]
  (->> (:username usr)
       (api/user-by-username)
       (api/remove-api-token))
  (resp-ok))

;; User handler

(defn initialize
  [{{:keys [body]} :parameters}]
  (if (api/admin-exists?)
    (resp-error 403 "Admin already exists")
    (let [user (merge body {:type "user" :role "admin"})
          username (:username user)
          password (:password user)]
      (cond
        (> 4 (count username)) (resp-error 400 "User must be at least 4 characters long.")
        (> 8 (count password)) (resp-error 400 "Password must be at least 8 characters long.")
        :else (let [response (api/create-user user)]
                (if (some? response)
                  (resp-created (select-keys response [:id]))
                  (resp-error 400 "User already exist")))))))

(defn me
  [{{:keys [usr]} :identity}]
  (-> (:username usr)
      (api/user-by-username)
      (select-keys [:username :email :role :api-token :service-dashboard :node-dashboard])
      (resp-ok)))

(defn users
  [_]
  (->> (api/users)
       (resp-ok)))

(defn user
  [{{:keys [path]} :parameters}]
  (let [user (api/user (:id path))]
    (if (some? user)
      (resp-ok user)
      (resp-error 404 "user doesn't exist"))))

(defn user-delete
  [{{:keys [path]} :parameters
    {:keys [usr]}  :identity}]
  (let [user (:username usr)]
    (if (= (:_id (api/user-by-username user))
           (:id path))
      (resp-error 400 "Operation not allowed")
      (let [user (api/user (:id path))]
        (api/delete-user (:id path))
        (api/delete-user-registries (:username user))
        (resp-ok)))))

(defn user-create
  [{{:keys [body]} :parameters}]
  (let [payload (assoc body :type "user")]
    (validate-parameters! (> (count (:username payload)) 3) "Username must be at least 4 characters long")
    (validate-parameters! (> (count (:password payload)) 3) "Password must be at least 4 characters long")
    (let [response (api/create-user payload)]
      (if (some? response)
        (resp-created (select-keys response [:id]))
        (resp-error 400 "User already exist")))))

(defn user-update
  [{{:keys [body path]} :parameters}]
  (api/update-user (:id path) body)
  (resp-ok))

;; Service handler

(defn services
  [_]
  (->> (api/services)
       (resp-ok)))

(defn services-ts-cpu
  [_]
  (if (stats/influx-configured?)
    (->> (stats/services-cpu-timeseries-memo)
         (resp-ok))
    (resp-error 400 "Statistics disabled")))

(defn services-ts-memory
  [_]
  (if (stats/influx-configured?)
    (->> (stats/services-memory-timeseries-memo)
         (resp-ok))
    (resp-error 400 "Statistics disabled")))

(defn service
  [{{:keys [path]} :parameters}]
  (->> (api/service (:id path))
       (resp-ok)))

(defn service-networks
  [{{:keys [path]} :parameters}]
  (->> (api/service-networks (:id path))
       (resp-ok)))

(defn service-tasks
  [{{:keys [path]} :parameters}]
  (->> (api/service-tasks (:id path))
       (resp-ok)))

(defn service-logs
  [{{:keys [path query]} :parameters}]
  (->> (:since query)
       (api/service-logs (:id path))
       (resp-ok)))

(defn service-create
  [{{:keys [body]} :parameters
    {:keys [usr]}  :identity}]
  (let [owner (:username usr)]
    (->> (api/create-service owner body)
         (resp-created))))

(defn service-update
  [{{:keys [body path]} :parameters
    {:keys [usr]}       :identity}]
  (let [owner (:username usr)]
    (api/update-service owner (:id path) body)
    (resp-ok)))

(defn service-redeploy
  [{{:keys [path query]} :parameters
    {:keys [usr]}        :identity}]
  (let [owner (:username usr)]
    (api/redeploy-service owner (:id path) (:tag query))
    (resp-accepted)))

(defn service-rollback
  [{{:keys [path]} :parameters
    {:keys [usr]}  :identity}]
  (let [owner (:username usr)]
    (api/rollback-service owner (:id path))
    (resp-accepted)))

(defn service-stop
  [{{:keys [path]} :parameters
    {:keys [usr]}  :identity}]
  (let [owner (:username usr)
        result (api/stop-service owner (:id path))]
    (resp-ok result)))

(defn service-delete
  [{{:keys [path]} :parameters}]
  (api/delete-service (:id path))
  (resp-ok))

(defn service-compose
  [{{:keys [path]} :parameters}]
  (let [response (api/service-compose (:id path))]
    (if (some? response)
      (resp-ok {:name (:name path)
                :spec {:compose response}})
      (resp-error 400 "Failed to create compose file"))))

(defn labels-service
  [_]
  (resp-ok (api/labels-service)))

;; Network handler

(defn networks
  [_]
  (->> (api/networks)
       (resp-ok)))

(defn network
  [{{:keys [path]} :parameters}]
  (->> (api/network (:id path))
       (resp-ok)))

(defn network-services
  [{{:keys [path]} :parameters}]
  (->> (api/services-by-network (:id path))
       (resp-ok)))

(defn network-create
  [{{:keys [body]} :parameters}]
  (->> (api/create-network body)
       (resp-created)))

(defn network-delete
  [{{:keys [path]} :parameters}]
  (api/delete-network (:id path))
  (resp-ok))

;; Volume handler

(defn volumes
  [_]
  (->> (api/volumes)
       (resp-ok)))

(defn volume
  [{{:keys [path]} :parameters}]
  (->> (api/volume (:name path))
       (resp-ok)))

(defn volume-services
  [{{:keys [path]} :parameters}]
  (->> (api/services-by-volume (:name path))
       (resp-ok)))

(defn volume-create
  [{{:keys [body]} :parameters}]
  (->> (api/create-volume body)
       (resp-created)))

(defn volume-delete
  [{{:keys [path]} :parameters}]
  (api/delete-volume (:name path))
  (resp-ok))

;; Secret handler

(defn secrets
  [_]
  (->> (api/secrets)
       (resp-ok)))

(defn secret
  [{{:keys [path]} :parameters}]
  (->> (api/secret (:id path))
       (resp-ok)))

(defn secret-services
  [{{:keys [path]} :parameters}]
  (->> (api/services-by-secret (:id path))
       (resp-ok)))

(defn secret-create
  [{{:keys [body]} :parameters}]
  (->> (api/create-secret body)
       (resp-created)))

(defn secret-delete
  [{{:keys [path]} :parameters}]
  (api/delete-secret (:id path))
  (resp-ok))

(defn secret-update
  [{{:keys [path body]} :parameters}]
  (api/update-secret (:id path) body)
  (resp-ok))

;; Config handler

(defn configs
  [_]
  (->> (api/configs)
       (resp-ok)))

(defn config
  [{{:keys [path]} :parameters}]
  (->> (api/config (:id path))
       (resp-ok)))

(defn config-services
  [{{:keys [path]} :parameters}]
  (->> (api/services-by-config (:id path))
       (resp-ok)))

(defn config-create
  [{{:keys [body]} :parameters}]
  (->> (api/create-config body)
       (resp-created)))

(defn config-delete
  [{{:keys [path]} :parameters}]
  (api/delete-config (:id path))
  (resp-ok))

;; Node handler

(defn nodes
  [_]
  (->> (api/nodes)
       (resp-ok)))

(defn nodes-ts
  [_]
  (if (stats/influx-configured?)
    (->> (stats/hosts-timeseries-memo)
         (resp-ok))
    (resp-error 400 "Statistics disabled")))

(defn node
  [{{:keys [path]} :parameters}]
  (->> (api/node (:id path))
       (resp-ok)))

(defn node-update
  [{{:keys [path body]} :parameters}]
  (api/update-node (:id path) body)
  (resp-ok))

(defn node-delete
  [{{:keys [path]} :parameters}]
  (api/delete-node (:id path))
  (resp-ok))

(defn node-tasks
  [{{:keys [path]} :parameters}]
  (->> (api/node-tasks (:id path))
       (resp-ok)))

;; Dashboard

(defn dashboard-pin
  [{{:keys [path]} :parameters
    {:keys [usr]}  :identity
    {:keys [data]} :reitit.core/match}]
  (let [user (api/user-by-username (:username usr))]
    (case (:name data)
      :service-dashboard (api/service (:id path))
      :node-dashboard (api/node (:id path)))
    (api/update-dashbboard user (:name data) conj (:id path))
    (resp-ok)))

(defn dashboard-detach
  [{{:keys [path]} :parameters
    {:keys [usr]}  :identity
    {:keys [data]} :reitit.core/match}]
  (let [user (api/user-by-username (:username usr))]
    (api/update-dashbboard user (:name data) disj (:id path))
    (resp-ok)))

;; Statistics

(defn stats
  [_]
  (if (stats/ready?)
    (->> (stats/cluster)
         (resp-ok))
    (resp-error 400 "Statistics not ready")))

;; Placement handler

(defn placement
  [_]
  (->> (api/placement)
       (resp-ok)))

;; Plugin handler

(defn plugin-network
  [_]
  (->> (api/plugins-by-type "Network")
       (filter #(not (contains? #{"null" "host"} %)))
       (resp-ok)))

(defn plugin-log
  [_]
  (->> (api/plugins-by-type "Log")
       (resp-ok)))

(defn plugin-volume
  [_]
  (->> (api/plugins-by-type "Volume")
       (resp-ok)))

;; Task handler

(defn tasks
  [_]
  (->> (api/tasks)
       (resp-ok)))

(defn task
  [{{:keys [path]} :parameters}]
  (->> (api/task (:id path))
       (resp-ok)))

(defn task-ts
  [{{:keys [path]} :parameters}]
  (if (stats/influx-configured?)
    (->> (stats/task-timeseries-memo (:name path))
         (resp-ok))
    (resp-error 400 "Statistics disabled")))

;; Registry handler

(defn registries
  [{{:keys [path]} :parameters
    {:keys [usr]}  :identity}]
  (let [owner (:username usr)
        registry (keyword (:registryType path))]
    (if (api/supported-registry-type? registry)
      (->> (case registry
             :v2 (api/registries-v2 owner)
             :dockerhub (api/dockerhubs owner)
             :ecr (api/registries-ecr owner)
             :acr (api/registries-acr owner)
             :gitlab (api/registries-gitlab owner))
           (resp-ok))
      (resp-error 400 (str "Unknown registry type [" registry "]")))))

(defn registry
  [{{:keys [path]} :parameters}]
  (let [id (:id path)
        registry (keyword (:registryType path))]
    (if (api/supported-registry-type? registry)
      (->> (case registry
             :v2 (api/registry-v2 id)
             :dockerhub (api/dockerhub id)
             :ecr (api/registry-ecr id)
             :acr (api/registry-acr id)
             :gitlab (api/registry-gitlab id))
           (resp-ok))
      (resp-error 400 (str "Unknown registry type [" registry "]")))))

(defn registry-delete
  [{{:keys [path]} :parameters}]
  (let [id (:id path)
        registry (keyword (:registryType path))]
    (if (api/supported-registry-type? registry)
      (do (case registry
            :v2 (api/delete-v2-registry id)
            :dockerhub (api/delete-dockerhub id)
            :ecr (api/delete-ecr-registry id)
            :acr (api/delete-acr-registry id)
            :gitlab (api/delete-gitlab-registry id))
          (resp-ok))
      (resp-error 400 (str "Unknown registry type [" registry "]")))))

;; TODO: Return 404 when invalid ID
(defn registry-repositories
  [{{:keys [path]} :parameters}]
  (let [id (:id path)
        registry (keyword (:registryType path))]
    (if (api/supported-registry-type? registry)
      (->> (case registry
             :v2 (api/registry-v2-repositories id)
             :dockerhub (api/dockerhub-repositories id)
             :ecr (api/registry-ecr-repositories id)
             :acr (api/registry-acr-repositories id)
             :gitlab (api/registry-gitlab-repositories id))
           (resp-ok))
      (resp-error 400 (str "Unknown registry type " registry)))))

;;; Registry CREATE handler

(defmulti registry-add (fn [registry payload] registry))

(defmethod registry-add :v2
  [_ payload]
  (try
    (api/registry-v2-info payload)
    (let [response (api/create-v2-registry payload)]
      (if (some? response)
        (resp-created (select-keys response [:id]))
        (resp-error 400 "Registry account already linked")))
    (catch Exception e
      (resp-error 400 (get-in (ex-data e) [:body :error])))))

(defmethod registry-add :dockerhub
  [_ payload]
  (let [dockeruser-token (:token (api/dockerhub-login payload))
        dockeruser-info (api/dockerhub-info payload)
        dockeruser-namespace (api/dockerhub-namespace dockeruser-token)
        response (api/create-dockerhub payload dockeruser-info dockeruser-namespace)]
    (if (some? response)
      (resp-created (select-keys response [:id]))
      (resp-error 400 "Dockerhub account already linked"))))

(defmethod registry-add :ecr
  [_ payload]
  (try
    (let [url (:proxyEndpoint (api/registry-ecr-token payload))
          response (api/create-ecr-registry (assoc payload :url url))]
      (if (some? response)
        (resp-created (select-keys response [:id]))
        (resp-error 400 "AWS ECR account already linked")))
    (catch Exception e
      (resp-error 400 (get-in (ex-data e) [:body :error])))))

(defmethod registry-add :acr
  [_ payload]
  (try
    (let [url (api/acr-url payload)
          payload (assoc payload :url url)
          info (api/registry-acr-info payload)
          response (api/create-acr-registry payload)]
      (if (some? response)
        (resp-created (select-keys response [:id]))
        (resp-error 400 "Azure ACR account with given service principals already linked")))
    (catch Exception e
      (resp-error 400 (get-in (ex-data e) [:body :error])))))

(defmethod registry-add :gitlab
  [_ payload]
  (try
    (api/registry-gitlab-info payload)
    (let [response (api/create-gitlab-registry payload)]
      (if (some? response)
        (resp-created (select-keys response [:id]))
        (resp-error 400 "Gitlab registry account already linked")))
    (catch Exception e
      (resp-error 400 (get-in (ex-data e) [:body :error])))))

(defn registry-create
  [{{:keys [path body]} :parameters
    {:keys [usr]}       :identity}]
  (let [owner (:username usr)
        registry (keyword (:registryType path))
        payload (assoc body :owner owner
                            :type registry)]
    (if (api/supported-registry-type? registry)
      (registry-add registry payload)
      (resp-error 400 (str "Unknown registry type [" registry "]")))))

;;; Registry UPDATE handler

(defmulti registry-edit (fn [registry payload id] registry))

(defmethod registry-edit :v2
  [_ payload id]
  (try
    (api/registry-v2-info payload)
    (api/update-v2-registry id payload)
    (resp-ok)
    (catch Exception e
      (resp-error 400 (get-in (ex-data e) [:body :error])))))

(defmethod registry-edit :dockerhub
  [_ payload id]
  (when (:password payload)
    (api/dockerhub-login payload))
  (api/update-dockerhub id payload)
  (resp-ok))

(defmethod registry-edit :ecr
  [_ payload id]
  (try
    (let [url (:proxyEndpoint (api/registry-ecr-token payload))
          delta-payload (assoc payload :url url)]
      (api/update-ecr-registry id delta-payload)
      (resp-ok))
    (catch Exception e
      (resp-error 400 (get-in (ex-data e) [:body :error])))))

(defmethod registry-edit :acr
  [_ payload id]
  (try
    (api/registry-acr-info payload)
    (api/update-acr-registry id payload)
    (resp-ok)
    (catch Exception e
      (resp-error 400 (get-in (ex-data e) [:body :error])))))

(defmethod registry-edit :gitlab
  [_ payload id]
  (try
    (api/registry-gitlab-info payload)
    (api/update-gitlab-registry id payload)
    (resp-ok)
    (catch Exception e
      (resp-error 400 (get-in (ex-data e) [:body :error])))))

(defn registry-update
  [{{:keys [path body]} :parameters}]
  (let [id (:id path)
        registry (keyword (:registryType path))]
    (if (api/supported-registry-type? registry)
      (registry-edit registry body id)
      (resp-error 400 (str "Unknown registry type [" registry "]")))))

;; Public dockerhub handler

(defn public-repositories
  [{{:keys [query]} :parameters}]
  (let [repository-query (:query query)
        repository-page (:page query)]
    (->> (api/public-repositories repository-query repository-page)
         (resp-ok))))

;; Repository handler

(defn repository-tags
  [{{:keys [query]} :parameters
    {:keys [usr]}   :identity}]
  (let [owner (:username usr)
        repository-name (:repository query)]
    (if (nil? repository-name)
      (resp-error 400 "Parameter name missing")
      (->> (api/repository-tags owner repository-name)
           (resp-ok)))))

(defn repository-ports
  [{{:keys [query]} :parameters
    {:keys [usr]}   :identity}]
  (let [owner (:username usr)
        repository-name (:repository query)
        repository-tag (:repositoryTag query)]
    (if (or (nil? repository-name)
            (nil? repository-tag))
      (resp-error 400 "Parameter name or tag missing")
      (->> (api/repository-ports owner repository-name repository-tag)
           (resp-ok)))))

;; Stack handler

(defn stacks
  [_]
  (->> (api/stacks)
       (resp-ok)))

(defn stack-create
  [{{:keys [body]} :parameters
    {:keys [usr]}  :identity}]
  (let [owner (:username usr)]
    (if (some? (api/stack (:name body)))
      (resp-error 400 "Stack already exist.")
      (do (api/create-stack owner body)
          (resp-created)))))

(defn stack-update
  [{{:keys [body path]} :parameters
    {:keys [usr]}       :identity}]
  (let [owner (:username usr)]
    (if (not= (:name path)
              (:name body))
      (resp-error 400 "Stack invalid.")
      (do (api/update-stack owner body)
          (resp-ok)))))

(defn stack-redeploy
  [{{:keys [path]} :parameters
    {:keys [usr]}  :identity}]
  (let [owner (:username usr)]
    (api/redeploy-stack owner (:name path))
    (resp-ok)))

(defn stack-rollback
  [{{:keys [path]} :parameters
    {:keys [usr]}  :identity}]
  (let [owner (:username usr)]
    (api/rollback-stack owner (:name path))
    (resp-ok)))

(defn stack-delete
  [{{:keys [path]} :parameters}]
  (let [{:keys [result]} (api/delete-stack (:name path))]
    (if (nil? (api/stack (:name path)))
      (do
        (api/delete-stackfile (:name path))
        (resp-ok))
      (resp-error 400 result))))

(defn stack-deactivate
  [{{:keys [path]} :parameters}]
  (let [{:keys [result]} (api/delete-stack (:name path))]
    (if (nil? (api/stack (:name path)))
      (resp-ok)
      (resp-error 400 result))))

(defn stack-file
  [{{:keys [path]} :parameters}]
  (let [response (api/stackfile (:name path))]
    (if (some? response)
      (resp-ok response)
      (resp-error 400 "Stackfile not found"))))

(defn stack-file-create
  [{{:keys [body path]} :parameters}]
  (if (= (:name path)
         (:name body))
    (let [stack (api/stackfile (:name path))]
      (if stack
        (resp-error 400 "Stack with given name already exist")
        (do
          (api/create-stackfile body)
          (resp-ok))))
    (resp-error 400 "Request mismatch")))

(defn stack-file-delete
  [{{:keys [path]} :parameters}]
  (api/delete-stackfile (:name path))
  (resp-ok))

(defn stack-compose
  [{{:keys [path]} :parameters}]
  (let [response (api/stack-compose (:name path))]
    (if (some? response)
      (resp-ok {:name (:name path)
                :spec {:compose response}})
      (resp-error 400 "Failed to create compose file"))))

(defn stack-services
  [{{:keys [path]} :parameters}]
  (-> (api/stack-services (:name path))
      (resp-ok)))

(defn stack-tasks
  [{{:keys [path]} :parameters}]
  (-> (api/stack-tasks (:name path))
      (resp-ok)))

(defn stack-networks
  [{{:keys [path]} :parameters}]
  (-> (api/stack-services (:name path))
      (api/networks-by-services)
      (resp-ok)))

(defn stack-volumes
  [{{:keys [path]} :parameters}]
  (-> (api/stack-services (:name path))
      (api/volumes-by-services)
      (resp-ok)))

(defn stack-configs
  [{{:keys [path]} :parameters}]
  (-> (api/stack-services (:name path))
      (api/resources-by-services :configs api/configs)
      (resp-ok)))

(defn stack-secrets
  [{{:keys [path]} :parameters}]
  (-> (api/stack-services (:name path))
      (api/resources-by-services :secrets api/secrets)
      (resp-ok)))
