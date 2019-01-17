(ns swarmpit.api
  (:require [clojure.set :refer [rename-keys]]
            [buddy.hashers :as hashers]
            [digest :refer [digest]]
            [swarmpit.utils :refer [merge-data]]
            [swarmpit.config :as cfg]
            [swarmpit.stats :as stats]
            [swarmpit.yaml :as yaml :refer [->yaml]]
            [swarmpit.docker.utils :as du]
            [swarmpit.docker.engine.client :as dc]
            [swarmpit.docker.engine.cli :as dcli]
            [swarmpit.docker.engine.log :as dl]
            [swarmpit.docker.engine.mapper.inbound :as dmi]
            [swarmpit.docker.engine.mapper.outbound :as dmo]
            [swarmpit.docker.engine.mapper.compose :refer [->compose]]
            [swarmpit.docker.auth.client :as dac]
            [swarmpit.docker.registry.client :as drc]
            [swarmpit.docker.hub.client :as dhc]
            [swarmpit.docker.hub.mapper.inbound :as dhmi]
            [swarmpit.registry.client :as rc]
            [swarmpit.registry.mapper.inbound :as rmi]
            [swarmpit.couchdb.client :as cc]
            [swarmpit.couchdb.mapper.inbound :as cmi]
            [swarmpit.couchdb.mapper.outbound :as cmo]
            [clojure.core.memoize :as memo]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [cemerick.url :refer [url]]
            [swarmpit.token :as token]))

;;; User API

(defn users
  []
  (-> (cc/users)
      (cmi/->users)))

(defn user
  [user-id]
  (-> (cc/user user-id)
      (cmi/->user)))

(defn user-by-username
  [username]
  (cc/user-by-username username))

(defn user-exist?
  [user]
  (some? (user-by-username (:username user))))

(defn delete-user
  [user-id]
  (->> (user user-id)
       (cc/delete-user)))

(defn create-user
  [user]
  (if (not (user-exist? user))
    (->> (cmo/->user user)
         (cc/create-user))))

(defn update-user
  [user-id user-delta]
  (->> (cc/update-user (cc/user user-id) user-delta)
       (cmi/->user)))

(defn change-password
  [user password]
  (->> (cmo/->password password)
       (cc/change-password user)))

(def password-check hashers/check)

(defn password-check-upgrade
  [password hash upgrade-f]
  (try
    (password-check password hash)
    (catch Exception e
      (if (= hash (digest "sha-256" password))
        (do (upgrade-f) true)
        false))))

(defn user-by-credentials
  [{:keys [username password]}]
  (let [user (user-by-username username)]
    (when
      (password-check-upgrade password (:password user)
                              #(change-password user password))
      user)))

(defn generate-api-token
  [user]
  (let [jti (swarmpit.uuid/uuid)
        token (token/generate-jwt user {:exp nil :jti jti :iss "swarmpit-api"})]
    (cc/set-api-token user {:jti jti :mask (subs token (- (count token) 5))})
    {:token token}))

(defn remove-api-token
  [user]
  (cc/set-api-token user nil))

;;; Secret API

(defn secrets
  ([]
   (secrets nil))
  ([label]
   (-> (dc/secrets label)
       (dmi/->secrets))))

(defn secret
  [secret-id]
  (-> (dc/secret secret-id)
      (dmi/->secret)))

(defn delete-secret
  [secret-id]
  (dc/delete-secret secret-id))

(defn create-secret
  [secret]
  (-> (dmo/->secret secret)
      (dc/create-secret)
      (rename-keys {:ID :id})))

(defn update-secret
  [secret-id secret]
  (let [secret-version (:version secret)]
    (->> (dmo/->secret secret)
         (dc/update-secret secret-id secret-version))))

;;; Config API

(defn configs
  ([]
   (configs nil))
  ([label]
   (try (-> (dc/configs label)
            (dmi/->configs))
        (catch Exception _ []))))

(defn config
  [config-id]
  (-> (dc/config config-id)
      (dmi/->config)))

(defn delete-config
  [config-id]
  (dc/delete-config config-id))

(defn create-config
  [config]
  (-> (dmo/->config config)
      (dc/create-config)
      (rename-keys {:ID :id})))

;;; Network API

(defn networks
  ([]
   (networks nil))
  ([label]
   (-> (dc/networks label)
       (dmi/->networks))))

(defn network
  [network-id]
  (-> (dc/network network-id)
      (dmi/->network)))

(defn delete-network
  [network-id]
  (dc/delete-network network-id))

(defn create-network
  [network]
  (-> (dmo/->network network)
      (dc/create-network)
      (rename-keys {:Id :id})))

;;; Volume API

(defn volumes
  ([]
   (volumes nil))
  ([label]
   (-> (dc/volumes label)
       (dmi/->volumes))))

(defn volume
  [volume-name]
  (-> (dc/volume volume-name)
      (dmi/->volume)))

(defn delete-volume
  [volume-name]
  (dc/delete-volume volume-name))

(defn create-volume
  [volume]
  (->> (dmo/->volume volume)
       (dc/create-volume)
       (dmi/->volume)))

;;; Stackfile API

(defn stackfiles
  []
  (cc/stackfiles))

(defn stackfile
  [stack-name]
  (cc/stackfile stack-name))

(defn delete-stackfile
  [stack-name]
  (-> (cc/stackfile stack-name)
      (cc/delete-stackfile)))

(defn- stackfile-json
  [stackfile-spec]
  (try
    (yaml/->json (:compose stackfile-spec))
    (catch Exception _ nil)))

(defn- stackfile-services
  [stackfile-spec]
  (->> (stackfile-json stackfile-spec)
       :services
       (vals)
       (map #(dmi/->service-image-details (:image %)))))

(defn- stackfile-distibutions
  [stackfile-spec filter-fx]
  (->> (stackfile-services stackfile-spec)
       (filter #(filter-fx (:name %)))
       (map #(du/distribution-id (:name %)))
       (into (hash-set))))

(defn- stackfile-dockerhub-distributions
  [stackfile-spec]
  (stackfile-distibutions stackfile-spec du/dockerhub?))

(defn- stackfile-registry-distributions
  [stackfile-spec]
  (stackfile-distibutions stackfile-spec du/registry?))

;;; Dockerhub registry API

(defn dockerusers
  [owner]
  (-> (cc/dockerusers owner)
      (cmi/->dockerusers)))

(defn dockeruser-info
  [dockeruser]
  (dhc/info dockeruser))

(defn dockeruser-namespace
  [dockeruser-token]
  (-> (dhc/namespaces dockeruser-token)
      :namespaces))

(defn dockeruser-login
  [dockeruser]
  (dhc/login dockeruser))

(defn dockeruser-exist?
  [dockeruser]
  (cc/dockeruser-exist? dockeruser))

(defn dockeruser
  [dockeruser-id]
  (-> (cc/dockeruser dockeruser-id)
      (cmi/->dockeruser)))

(defn- dockeruser-by-namespace
  [owner dockeruser-namespace]
  (->> (cc/dockerusers owner)
       (filter #(contains? (set (:namespaces %)) dockeruser-namespace))
       (first)))

(defn- dockeruser-by-stackfile
  "Return best matching dockerhub account for given stackfile spec"
  [owner stackfile-spec]
  (let [namespaces (stackfile-dockerhub-distributions stackfile-spec)]
    (->> (cc/dockerusers owner)
         (map #(hash-map
                 :user-id (:_id %)
                 :matches (get
                            (->> (:namespaces %)
                                 (map (fn [ns] (contains? namespaces ns)))
                                 (frequencies))
                            true)))
         (filter #(some? (:matches %)))
         (sort-by :matches)
         (last)
         :user-id
         (cc/dockeruser))))

(defn create-dockeruser
  [dockeruser dockeruser-info dockeruser-namespace]
  (if (not (dockeruser-exist? dockeruser))
    (->> (cmo/->docker-user dockeruser dockeruser-info dockeruser-namespace)
         (cc/create-dockeruser))))

(defn update-dockeruser
  [dockeruser-id dockeruser-delta]
  (->> (cc/update-dockeruser (cc/dockeruser dockeruser-id) dockeruser-delta)
       (cmi/->dockeruser)))

(defn delete-dockeruser
  [dockeruser-id]
  (-> (dockeruser dockeruser-id)
      (cc/delete-dockeruser)))

(defn dockeruser-repositories
  [dockeruser-id]
  (let [dockeruser (cc/dockeruser dockeruser-id)
        dockeruser-token (:token (dhc/login dockeruser))]
    (->> (dhc/namespaces dockeruser-token)
         :namespaces
         (map #(:results (dhc/repositories-by-namespace dockeruser-token %)))
         (flatten)
         (dhmi/->user-repositories))))

;; Public repository API

(defn public-repositories
  [repository-query repository-page]
  (-> (dhc/repositories repository-query repository-page)
      (dhmi/->repositories repository-query repository-page)))

;;; Registry v2 API

(defn registries
  [owner]
  (-> (cc/registries owner)
      (cmi/->registries)))

(defn- registries-by-stackfile
  "Return registry accounts for given stackfile spec"
  [owner stackfile-spec]
  (let [urls (stackfile-registry-distributions stackfile-spec)]
    (->> (cc/registries owner)
         (filter #(contains? urls (-> % :url url :host))))))

(defn registry
  [registry-id]
  (-> (cc/registry registry-id)
      (cmi/->registry)))

(defn- registry-by-url
  [owner registry-address]
  (let [registry (->> (cc/registries owner)
                      (filter #(.contains (:url %) registry-address))
                      (first))]
    (if (nil? registry)
      (throw
        (ex-info "Registry error: authentication required"
                 {:status 401
                  :body   {:error "authentication required"}}))
      registry)))

(defn registry-exist?
  [registry]
  (cc/registry-exist? registry))

(defn registry-info
  [{:keys [_id password] :as registry}]
  (let [old-passwd (:password (cc/registry _id))]
    (if password
      (rc/info registry)
      (rc/info (assoc registry :password old-passwd)))))

(defn delete-registry
  [registry-id]
  (->> (registry registry-id)
       (cc/delete-registry)))

(defn create-registry
  [registry]
  (when (not (registry-exist? registry))
    (cc/create-registry registry)))

(defn update-registry
  [registry-id registry-delta]
  (->> (cc/update-registry (cc/registry registry-id) registry-delta)
       (cmi/->registry)))

(defn registry-repositories
  [registry-id]
  (->> (cc/registry registry-id)
       (rc/repositories)
       (rmi/->repositories)))

;;; Repository Tags API

(defn- registry-repository-tags
  [owner repository-name]
  (let [registry-address (du/distribution-id repository-name)
        repository-name (du/registry-repository repository-name registry-address)]
    (-> (registry-by-url owner registry-address)
        (rc/tags repository-name)
        :tags)))

(defn- dockeruser-repository-tags
  [owner repository-name]
  (let [dockeruser-namespace (du/distribution-id repository-name)]
    (-> (dockeruser-by-namespace owner dockeruser-namespace)
        (dac/token repository-name)
        :token
        (drc/tags repository-name)
        :tags)))

(defn- library-repository-tags
  [repository-name]
  (let [repository-name (du/library-repository repository-name)]
    (-> (dac/token nil repository-name)
        :token
        (drc/tags repository-name)
        :tags)))

(defn repository-tags
  [owner repository-name]
  (cond
    (du/library? repository-name) (library-repository-tags repository-name)
    (du/dockerhub? repository-name) (dockeruser-repository-tags owner repository-name)
    :else (registry-repository-tags owner repository-name)))

;;; Repository Ports API

(defn- registry-repository-ports
  [owner repository-name repository-tag]
  (let [registry-address (du/distribution-id repository-name)
        repository-name (du/registry-repository repository-name registry-address)]
    (-> (registry-by-url owner registry-address)
        (rc/manifest repository-name repository-tag)
        (rmi/->repository-config)
        :config
        (dmi/->image-ports))))

(defn- dockeruser-repository-ports
  [owner repository-name repository-tag]
  (let [dockeruser-namespace (du/distribution-id repository-name)]
    (-> (dockeruser-by-namespace owner dockeruser-namespace)
        (dac/token repository-name)
        :token
        (drc/manifest repository-name repository-tag)
        (rmi/->repository-config)
        :config
        (dmi/->image-ports))))

(defn- library-repository-ports
  [repository-name repository-tag]
  (let [repository-name (du/library-repository repository-name)]
    (-> (dac/token nil repository-name)
        :token
        (drc/manifest repository-name repository-tag)
        (rmi/->repository-config)
        :config
        (dmi/->image-ports))))

(defn repository-ports
  [owner repository-name repository-tag]
  (cond
    (du/library? repository-name) (library-repository-ports repository-name repository-tag)
    (du/dockerhub? repository-name) (dockeruser-repository-ports owner repository-name repository-tag)
    :else (registry-repository-ports owner repository-name repository-tag)))

;;; Repository Digest API

(defn- registry-repository-digest
  [owner repository-name repository-tag]
  (let [registry-address (du/distribution-id repository-name)
        repository-name (du/registry-repository repository-name registry-address)]
    (-> (registry-by-url owner registry-address)
        (rc/digest repository-name repository-tag))))

(defn- dockeruser-repository-digest
  [owner repository-name repository-tag]
  (let [dockeruser-namespace (du/distribution-id repository-name)]
    (-> (dockeruser-by-namespace owner dockeruser-namespace)
        (dac/token repository-name)
        :token
        (drc/digest repository-name repository-tag))))

(defn- library-repository-digest
  [repository-name repository-tag]
  (let [repository-name (du/library-repository repository-name)]
    (-> (dac/token nil repository-name)
        :token
        (drc/digest repository-name repository-tag))))

(defn repository-digest
  [owner repository-name repository-tag]
  (cond
    (du/library? repository-name) (library-repository-digest repository-name repository-tag)
    (du/dockerhub? repository-name) (dockeruser-repository-digest owner repository-name repository-tag)
    :else (registry-repository-digest owner repository-name repository-tag)))

;;; Task API

(defn task-stats
  [task]
  (let [stats (apply dissoc (stats/task task) [:name :id])]
    (assoc task :stats stats)))

(defn tasks
  []
  (->> (dmi/->tasks (dc/tasks)
                    (dc/nodes)
                    (dc/services))
       (map #(task-stats %))))

(def tasks-memo (memo/ttl tasks :ttl/threshold 1000))

(defn task
  [task-id]
  (-> (dmi/->task (dc/task task-id)
                  (dc/nodes)
                  (dc/services))
      (task-stats)))

;;; Service API

(defn services
  ([]
   (services nil))
  ([label]
   (services label (dc/networks)))
  ([label networks]
   (dmi/->services (dc/services label)
                   (dc/tasks)
                   networks)))

(defn resources-by-services
  [services resource source]
  (let [ids (->> services
                 (map resource)
                 (flatten)
                 (map :id)
                 (set))]
    (->> (source)
         (filter #(contains? ids (:id %)))
         (vec))))

(defn volumes-by-services
  [services]
  (let [volumes (->> (volumes)
                     (group-by :id))]
    (->> services
         (map :mounts)
         (flatten)
         (filter #(= "volume" (:type %)))
         (map #(merge % {:volumeName (:host %)
                         :driver     (-> % :volumeOptions :driver :name)
                         :options    (-> % :volumeOptions :options)}))
         (map #(merge (first (get volumes (:id %))) %)))))

(def services-memo (memo/ttl services :ttl/threshold 1000))

(defn- services-by
  [service-filter]
  (dmi/->services (filter #(service-filter %) (dc/services))
                  (dc/tasks)
                  (dc/networks)))

(defn services-by-network
  [network-name]
  (services-by #(contains? (->> (get-in % [:Spec :TaskTemplate :Networks])
                                (map :Target)
                                (set)) (:id (network network-name)))))

(defn services-by-volume
  [volume-name]
  (services-by #(contains? (->> (get-in % [:Spec :TaskTemplate :ContainerSpec :Mounts])
                                (map :Source)
                                (set)) volume-name)))

(defn services-by-secret
  [secret-name]
  (services-by #(contains? (->> (get-in % [:Spec :TaskTemplate :ContainerSpec :Secrets])
                                (map :SecretName)
                                (set)) secret-name)))

(defn services-by-config
  [config-name]
  (services-by #(contains? (->> (get-in % [:Spec :TaskTemplate :ContainerSpec :Configs])
                                (map :ConfigName)
                                (set)) config-name)))

(defn service
  [service-id]
  (dmi/->service (dc/service service-id)
                 (dc/service-tasks service-id)
                 (dc/networks)))

(defn service-networks
  [service-id]
  (dmi/->service-networks (dc/service service-id)
                          (dc/networks)))

(defn service-tasks
  [service-id]
  (->> (dmi/->tasks (dc/service-tasks service-id)
                    (dc/nodes)
                    (dc/services))
       (map #(task-stats %))))

(defn service-logs
  [service-id from-timestamp]
  (letfn [(log-task [log tasks] (->> tasks
                                     (filter #(= (:task log) (:id %)))
                                     (first)))]
    (let [tasks (tasks)]
      (->> (dc/service-logs service-id)
           (dl/parse-log)
           (filter #(= 1 (compare (:timestamp %) from-timestamp)))
           (map
             (fn [i]
               (let [task (log-task i tasks)]
                 (-> i
                     (assoc :taskName (:taskName task))
                     (assoc :taskNode (:nodeName task))))))))))

(defn delete-service
  [service-id]
  (dc/delete-service service-id))

(defn- standardize-service-configs
  [service]
  (if (<= 1.30 (read-string (cfg/config :docker-api)))
    (assoc-in service [:configs] (dmo/->service-configs service (configs)))
    service))

(defn- standardize-service-secrets
  [service]
  (assoc-in service [:secrets] (dmo/->service-secrets service (secrets))))

(defn- standardize-repository-tag
  [repository-tag]
  (if (str/blank? repository-tag)
    "latest"
    repository-tag))

(defn- standardize-service
  [owner service]
  (let [repository-name (get-in service [:repository :name])
        repository-tag (standardize-repository-tag (get-in service [:repository :tag]))]
    (-> service
        (standardize-service-secrets)
        (standardize-service-configs)
        (assoc-in [:repository :tag] repository-tag)
        (assoc-in [:repository :imageDigest] (repository-digest owner
                                                                repository-name
                                                                repository-tag)))))

(defn- service-auth
  [owner service]
  (let [repository-name (get-in service [:repository :name])
        distribution-id (du/distribution-id repository-name)]
    (dmo/->auth-config
      (cond
        (du/library? repository-name) nil
        (du/dockerhub? repository-name) (dockeruser-by-namespace owner distribution-id)
        :else (registry-by-url owner distribution-id)))))

(defn create-service
  [owner service]
  (rename-keys
    (->> (standardize-service owner service)
         (dmo/->service)
         (dc/create-service (service-auth owner service))) {:ID :id}))

(defn- merge-service
  [service-origin service-delta]
  (-> (merge-data service-origin service-delta)
      (assoc-in [:Labels] (:Labels service-delta))))

(defn update-service
  [owner service]
  (let [standardized-service (standardize-service owner service)
        service-origin (-> (dc/service (:id service)) :Spec)
        service-delta (dmo/->service standardized-service)]
    (dc/update-service (service-auth owner service)
                       (:id service)
                       (:version service)
                       (merge-service service-origin service-delta))))

(defn redeploy-service
  [owner service-id]
  (let [service-origin (dc/service service-id)
        service (dmi/->service service-origin)
        repository-name (get-in service [:repository :name])
        repository-tag (get-in service [:repository :tag])
        image-digest (repository-digest owner repository-name repository-tag)
        image (str repository-name ":" repository-tag "@" image-digest)]
    (dc/update-service
      (service-auth owner service)
      service-id
      (get-in service-origin [:Version :Index])
      (-> service-origin
          :Spec
          (update-in [:TaskTemplate :ForceUpdate] inc)
          (assoc-in [:TaskTemplate :ContainerSpec :Image] image)))))

(defn rollback-service
  [owner service-id]
  (let [service-origin (dc/service service-id)
        service (dmi/->service service-origin)]
    (dc/update-service
      (service-auth owner service)
      service-id
      (get-in service-origin [:Version :Index])
      (-> service-origin
          :PreviousSpec))))

;;; Node API

(defn node-stats
  [node]
  (let [stats (apply dissoc (stats/node (:id node)) [:id :tasks])]
    (assoc node :stats stats)))

(defn nodes
  []
  (->> (dc/nodes)
       (dmi/->nodes)
       (map #(node-stats %))))

(defn node
  [node-id]
  (-> (dc/node node-id)
      (dmi/->node)
      (node-stats)))

(defn update-node
  [node]
  (dc/update-node (:id node)
                  (:version node)
                  node))

(defn update-node
  [node-id node]
  (let [node-version (:version node)]
    (->> (dmo/->node node)
         (dc/update-node node-id node-version))))

(defn node-tasks
  [node-id]
  (->> (dmi/->tasks (dc/node-tasks node-id)
                    (dc/nodes)
                    (dc/services))
       (map #(task-stats %))))

;; Labels API

(defn labels-service
  []
  (->> (services)
       (map #(->> % :labels
                  (map :name)
                  (map str/trim)
                  (set)))
       (apply clojure.set/union)))

;; Plugin API

(defn plugins
  []
  (->> (dc/nodes)
       (map #(get-in % [:Description :Engine :Plugins]))
       (flatten)
       (distinct)
       (group-by :Type)))

(defn plugins-by-type
  [type]
  (->> (get (plugins) type)
       (map :Name)))

;; Placement API

(defn- placement-rule
  [nodes-attribute node-rule-fn]
  (->> nodes-attribute
       (map #(for [x [" == " " != "]]
               (node-rule-fn x %)))
       (flatten)))

(defn placement
  []
  (let [nodes (nodes)
        nodes-id (map :id nodes)
        nodes-role '("manager" "worker")
        nodes-hostname (map :nodeName nodes)
        nodes-label (set (flatten (map :labels nodes)))]
    (concat
      (placement-rule
        nodes-id
        (fn [matcher item]
          (str "node.id" matcher item)))
      (placement-rule
        nodes-role
        (fn [matcher item]
          (str "node.role" matcher item)))
      (placement-rule
        nodes-hostname
        (fn [matcher item]
          (str "node.hostname" matcher item)))
      (placement-rule
        nodes-label
        (fn [matcher item]
          (str "node.labels." (:name item) matcher (:value item)))))))

;; Stack API

(defn stack-label
  [stack-name]
  (str "com.docker.stack.namespace=" stack-name))

(defn stack-services
  [stack-name]
  (-> (stack-label stack-name)
      (services)))

(defn stack
  ([stack-name services]
   (when (not-empty services)
     {:stackName stack-name
      :stackFile (some? (stackfile stack-name))
      :services  services
      :networks  (resources-by-services services :networks networks)
      :volumes   (volumes-by-services services)
      :configs   (resources-by-services services :configs configs)
      :secrets   (resources-by-services services :secrets secrets)}))
  ([stack-name]
   (stack stack-name (stack-services stack-name))))

(defn stack-compose
  [stack-name]
  (some-> (stack stack-name) (->compose) (->yaml)))

(defn service-compose
  [service-name]
  (some->> [(service service-name)]
           (stack nil)
           (->compose)
           (->yaml)))

(defn stacks
  []
  (->> (dissoc (group-by :stack (services)) nil)
       (map (fn [s]
              (letfn [(distinct-resources [r]
                        (->> (dissoc (group-by :id r) nil)
                             (vals)
                             (map first)
                             (map #(dissoc % :serviceAliases))))]
                (let [stack-name (key s)
                      stack-services (val s)
                      stack-networks (flatten (map :networks stack-services))
                      stack-volumes (flatten (map :mounts stack-services))
                      stack-configs (flatten (map :configs stack-services))
                      stack-secrets (flatten (map :secrets stack-services))]
                  (when (not-empty stack-services)
                    {:stackName stack-name
                     :stackFile (some? (stackfile stack-name))
                     :services  stack-services
                     :networks  (distinct-resources stack-networks)
                     :volumes   (distinct-resources stack-volumes)
                     :configs   (distinct-resources stack-configs)
                     :secrets   (distinct-resources stack-secrets)})))))))

(defn stack-login
  [owner stackfile-spec]
  (let [distributions (remove nil?
                              (conj (registries-by-stackfile owner stackfile-spec)
                                    (dockeruser-by-stackfile owner stackfile-spec)))]
    (doseq [distro distributions]
      (dcli/login (:username distro)
                  (:password distro)
                  (:url distro)))))

(defn create-stack
  "Create application stack and link stackfile"
  [owner {:keys [name spec] :as stackfile}]
  (let [stackfile-origin (cc/stackfile name)]
    (stack-login owner spec)
    (dcli/stack-deploy name (:compose spec))
    (if (some? stackfile-origin)
      (cc/update-stackfile stackfile-origin stackfile)
      (cc/create-stackfile stackfile))))

(defn update-stack
  "Update application stack and stackfile accordingly"
  [owner {:keys [name spec] :as stackfile}]
  (let [stackfile-origin (cc/stackfile name)]
    (stack-login owner spec)
    (dcli/stack-deploy name (:compose spec))
    (if (some? stackfile-origin)
      (cc/update-stackfile stackfile-origin {:spec         spec
                                             :previousSpec (:spec stackfile-origin)})
      (cc/create-stackfile stackfile))))

(defn redeploy-stack
  "Redeploy application stack"
  [owner name]
  (let [{:keys [name spec]} (cc/stackfile name)]
    (stack-login owner spec)
    (dcli/stack-deploy name (:compose spec))))

(defn rollback-stack
  "Rollback application stack and update stackfile accordingly"
  [owner name]
  (let [{:keys [name spec previousSpec] :as stackfile-origin} (cc/stackfile name)]
    (stack-login owner previousSpec)
    (dcli/stack-deploy name (:compose previousSpec))
    (cc/update-stackfile stackfile-origin {:spec         previousSpec
                                           :previousSpec spec})))

(defn delete-stack
  [stack-name]
  (dcli/stack-remove stack-name))