(ns swarmpit.api
  (:require [clojure.set :refer [rename-keys]]
            [buddy.hashers :as hashers]
            [digest :refer [digest]]
            [swarmpit.utils :refer [merge-data]]
            [swarmpit.config :as cfg]
            [swarmpit.time :as time]
            [swarmpit.stats :as stats]
            [swarmpit.base64 :as base64]
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
            [swarmpit.gitlab.client :as gc]
            [swarmpit.aws.client :as awsc]
            [swarmpit.agent.client :as sac]
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

(defn admin-exists?
  []
  (not (empty? (filter token/admin? (users)))))

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

(defn delete-user-registries
  [username]
  (cc/delete-user-registries username))

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

(defn update-dashbboard
  [user dashboard-type update-fn resource]
  (cc/update-dashboard
    user
    dashboard-type
    (-> (get user dashboard-type)
        (set)
        (update-fn resource))))

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
  (-> (dmo/->volume volume)
      (dc/create-volume)
      (dmi/->volume)))

;;; Dockerhub registry API

(defn dockerhubs
  [owner]
  (-> (cc/dockerhubs owner)
      (cmi/->dockerhubs)))

(defn dockerhub-info
  [dockeruser]
  (dhc/info dockeruser))

(defn dockerhub-namespace
  [dockeruser-token]
  (-> (dhc/namespaces dockeruser-token)
      :namespaces))

(defn dockerhub-login
  [dockeruser]
  (dhc/login dockeruser))

(defn dockerhub
  [dockeruser-id]
  (-> (cc/dockerhub dockeruser-id)
      (cmi/->dockerhub)))

(defn create-dockerhub
  [dockeruser dockeruser-info dockeruser-namespace]
  (->> (cmo/->dockerhub dockeruser dockeruser-info dockeruser-namespace)
       (cc/create-dockerhub)))

(defn update-dockerhub
  [dockeruser-id dockeruser-delta]
  (->> (cc/update-dockerhub (cc/dockerhub dockeruser-id) dockeruser-delta)
       (cmi/->dockerhub)))

(defn delete-dockerhub
  [dockeruser-id]
  (-> (dockerhub dockeruser-id)
      (cc/delete-dockerhub)))

(defn dockerhub-repositories
  [dockeruser-id]
  (let [dockeruser (cc/dockerhub dockeruser-id)
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

(defn registries-v2
  [owner]
  (-> (cc/registries-v2 owner)
      (cmi/->registries)))

(defn registry-v2
  [registry-id]
  (-> (cc/registry-v2 registry-id)
      (cmi/->registry)))

(defn registry-v2-info
  [{:keys [_id password] :as registry}]
  (let [old-passwd (:password (cc/registry-v2 _id))]
    (if password
      (rc/info registry)
      (rc/info (assoc registry :password old-passwd)))))

(defn delete-v2-registry
  [registry-id]
  (->> (registry-v2 registry-id)
       (cc/delete-v2-registry)))

(defn create-v2-registry
  [registry]
  (cc/create-v2-registry registry))

(defn update-v2-registry
  [registry-id registry-delta]
  (->> (cc/update-v2-registry (cc/registry-v2 registry-id) registry-delta)
       (cmi/->registry)))

(defn registry-v2-repositories
  [registry-id]
  (->> (cc/registry-v2 registry-id)
       (rc/repositories)
       (rmi/->repositories)))

;;; Registry AWS ECR API

(defn registries-ecr
  [owner]
  (-> (cc/registries-ecr owner)
      (cmi/->ecrs)))

(defn registry-ecr
  [ecr-id]
  (-> (cc/registry-ecr ecr-id)
      (cmi/->ecr)))

(defn registry-ecr-token
  [{:keys [_id accessKey] :as ecr}]
  (let [old-access-key (:accessKey (cc/registry-ecr _id))]
    (if accessKey
      (awsc/ecr-token ecr)
      (awsc/ecr-token (assoc ecr :accessKey old-access-key)))))

(defn registry-ecr-password
  [ecr]
  (-> (registry-ecr-token ecr)
      :authorizationToken
      (base64/decode)
      (str/split #":")
      (second)))

(defn create-ecr-registry
  [ecr]
  (cc/create-ecr-registry ecr))

(defn update-ecr-registry
  [ecr-id ecr-delta]
  (->> (cc/update-ecr-registry (cc/registry-ecr ecr-id) ecr-delta)
       (cmi/->ecr)))

(defn delete-ecr-registry
  [ecr-id]
  (->> (registry-ecr ecr-id)
       (cc/delete-ecr-registry)))

(defn registry-ecr->v2
  [ecr]
  (-> ecr
      (assoc :username "AWS")
      (assoc :password (registry-ecr-password ecr))
      (assoc :withAuth true)))

(defn registry-ecr-repositories
  [ecr-id]
  (->> (cc/registry-ecr ecr-id)
       (registry-ecr->v2)
       (rc/repositories)
       (rmi/->repositories)))

;;; Registry Azure ACR API

(defn registries-acr
  [owner]
  (-> (cc/registries-acr owner)
      (cmi/->acrs)))

(defn registry-acr
  [acr-id]
  (-> (cc/registry-acr acr-id)
      (cmi/->acr)))

(defn acr-url
  [acr]
  (str "https://" (:name acr) ".azurecr.io"))

(defn registry-acr->v2
  [acr]
  (-> acr
      (assoc :username (:spId acr))
      (assoc :password (:spPassword acr))
      (assoc :withAuth true)))

(defn registry-acr-info
  [{:keys [_id spPassword] :as acr}]
  (let [old-passwd (:spPassword (cc/registry-acr _id))
        acr-v2 (registry-acr->v2 acr)]
    (if spPassword
      (rc/info acr-v2)
      (rc/info (assoc acr-v2 :password old-passwd)))))

(defn create-acr-registry
  [acr]
  (cc/create-acr-registry acr))

(defn update-acr-registry
  [acr-id acr-delta]
  (->> (cc/update-acr-registry (cc/registry-acr acr-id) acr-delta)
       (cmi/->acr)))

(defn delete-acr-registry
  [ecr-id]
  (->> (registry-acr ecr-id)
       (cc/delete-acr-registry)))

(defn registry-acr-repositories
  [ecr-id]
  (->> (cc/registry-acr ecr-id)
       (registry-acr->v2)
       (rc/repositories)
       (rmi/->repositories)))

;;; Registry Gitlab API

(defn registries-gitlab
  [owner]
  (-> (cc/registries-gitlab owner)
      (cmi/->gitlab-registries)))

(defn registry-gitlab
  [registry-id]
  (-> (cc/registry-gitlab registry-id)
      (cmi/->gitlab-registry)))

(defn registry-gitlab->v2
  [registry]
  (-> registry
      (assoc :password (:token registry))
      (assoc :withAuth true)))

(defn registry-gitlab-info
  [{:keys [_id token] :as registry}]
  (let [old-passwd (:token (cc/registry-gitlab _id))
        gitlab-v2 (registry-gitlab->v2 registry)]
    (if token
      (rc/info gitlab-v2)
      (rc/info (assoc gitlab-v2 :password old-passwd)))))

(defn create-gitlab-registry
  [registry]
  (cc/create-gitlab-registry registry))

(defn update-gitlab-registry
  [registry-id registry-delta]
  (->> (cc/update-gitlab-registry (cc/registry-gitlab registry-id) registry-delta)
       (cmi/->gitlab-registry)))

(defn delete-gitlab-registry
  [registry-id]
  (->> (registry-gitlab registry-id)
       (cc/delete-gitlab-registry)))

(defn gitlab-group-projects
  [registry]
  (flatten
    (map
      (fn [x]
        (gc/group-projects registry (:id x)))
      (gc/groups registry))))

(defn gitlab-membership-projects
  [registry]
  (gc/projects registry))

(defn gitlab-projects
  [registry]
  (into #{}
        (concat
          (gitlab-group-projects registry)
          (gitlab-membership-projects registry))))

(def gitlab-projects-memo
  (memo/ttl
    gitlab-projects :ttl/threshold 3600000))

(defn registry-gitlab-repositories
  [registry-id]
  (let [registry (cc/registry-gitlab registry-id)]
    (->> (gitlab-projects-memo registry)
         (map (fn [x]
                (gc/project-repositories registry (:id x))))
         (flatten)
         (map (fn [{:keys [path]}]
                (into {:id   (hash path)
                       :name path}))))))

;;; Stackfile API

(defn stackfiles
  []
  (cc/stackfiles))

(defn stackfile
  [stack-name]
  (cc/stackfile stack-name))

(defn create-stackfile
  [{:keys [name spec] :as stackfile}]
  (cc/create-stackfile stackfile))

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

;;; Registry lookup API

(defn- dockerhub-by-namespace
  "Return dockerhub account matching given namespace"
  [owner dockeruser-namespace]
  (->> (cc/dockerhubs owner)
       (filter #(contains? (set (:namespaces %)) dockeruser-namespace))
       (first)))

(defn- dockerhub-by-stackfile
  "Return best matching dockerhub account for given stackfile spec"
  [owner stackfile-spec]
  (let [namespaces (stackfile-dockerhub-distributions stackfile-spec)]
    (->> (cc/dockerhubs owner)
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
         (cc/dockerhub))))

(defn- supported-registries
  "List all supported v2 type registries"
  [owner]
  (concat (cc/registries-v2 owner)
          (cc/registries-ecr owner)
          (cc/registries-acr owner)
          (cc/registries-gitlab owner)))

(def supported-registry-types #{:dockerhub :v2 :ecr :acr :gitlab})

(defn supported-registry-type? [type]
  (contains? supported-registry-types type))

(defn- registry-by-url
  "Return registry account matching given url"
  [owner registry-address]
  (let [registry (->> (supported-registries owner)
                      (filter #(.contains (:url %) registry-address))
                      (first))]
    (if (nil? registry)
      (throw
        (ex-info "Registry error: No matching registry linked with Swarmpit"
                 {:status 401
                  :type   :api
                  :body   {:error (str "No matching registry ( " registry-address " ) linked with Swarmpit")}}))
      (case (:type registry)
        "ecr" (registry-ecr->v2 registry)
        "acr" (registry-acr->v2 registry)
        "gitlab" (registry-gitlab->v2 registry)
        registry))))

(defn- registries-by-stackfile
  "Return registry accounts for given stackfile spec"
  [owner stackfile-spec]
  (let [urls (stackfile-registry-distributions stackfile-spec)]
    (->> (supported-registries owner)
         (filter #(contains? urls (-> % :url url :host))))))

;;; Repository Tags API

(defn- registry-repository-tags
  [owner repository-name]
  (let [registry-address (du/distribution-id repository-name)
        repository-name (du/registry-repository repository-name registry-address)]
    (-> (registry-by-url owner registry-address)
        (rc/tags repository-name)
        :tags)))

(defn- dockerhub-repository-tags
  [owner repository-name]
  (let [dockeruser-namespace (du/distribution-id repository-name)]
    (-> (dockerhub-by-namespace owner dockeruser-namespace)
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
    (du/dockerhub? repository-name) (dockerhub-repository-tags owner repository-name)
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

(defn- dockerhub-repository-ports
  [owner repository-name repository-tag]
  (let [dockeruser-namespace (du/distribution-id repository-name)]
    (-> (dockerhub-by-namespace owner dockeruser-namespace)
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
    (du/dockerhub? repository-name) (dockerhub-repository-ports owner repository-name repository-tag)
    :else (registry-repository-ports owner repository-name repository-tag)))

;;; Repository Digest API

(defn- registry-repository-digest
  [owner repository-name repository-tag]
  (let [registry-address (du/distribution-id repository-name)
        repository-name (du/registry-repository repository-name registry-address)]
    (-> (registry-by-url owner registry-address)
        (rc/digest repository-name repository-tag))))

(defn- dockerhub-repository-digest
  [owner repository-name repository-tag]
  (let [dockeruser-namespace (du/distribution-id repository-name)]
    (-> (dockerhub-by-namespace owner dockeruser-namespace)
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
    (du/dockerhub? repository-name) (dockerhub-repository-digest owner repository-name repository-tag)
    :else (registry-repository-digest owner repository-name repository-tag)))

;;; Task API

(defn task-stats
  [task]
  (let [stats (apply dissoc (stats/task task) [:name :id])]
    (-> task
        (assoc-in [:stats] stats))))

(defn tasks
  []
  (->> (dmi/->tasks (dc/tasks)
                    (dc/nodes)
                    (dc/services)
                    (dc/info))
       (map #(task-stats %))))

(def tasks-memo (memo/ttl tasks :ttl/threshold 1000))

(defn task
  [task-id]
  (-> (dmi/->task (dc/task task-id)
                  (dc/nodes)
                  (dc/services)
                  (dc/info))
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
                   networks
                   (dc/info))))

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

(defn networks-by-services
  [services]
  (let [local-networks (->> services
                            (map :networks)
                            (flatten)
                            (filter #(= "local" (:scope %)))
                            (set))]
    (resources-by-services services :networks #(concat (networks) local-networks))))

(defn volumes-by-services
  [services]
  (let [volumes (->> (volumes)
                     (group-by :id))]
    (->> services
         (map :mounts)
         (flatten)
         (filter #(= "volume" (:type %)))
         (map #(dissoc % :containerPath))
         (set)
         (map #(merge % {:volumeName (:host %)}))
         (map #(merge (first (get volumes (:id %))) %)))))

(def services-memo (memo/ttl services :ttl/threshold 1000))

(defn- services-by
  [service-filter]
  (dmi/->services (filter #(service-filter %) (dc/services))
                  (dc/tasks)
                  (dc/networks)
                  (dc/info)))

(defn services-by-network
  [network-name]
  (->> (services-by any?)
       (filter #(get (->> (get % :networks)
                          (group-by :networkName))
                     (:networkName (network network-name))))))

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
                 (dc/networks)
                 (dc/info)))

(defn service-networks
  [service-id]
  (dmi/->service-networks (dc/service service-id)
                          (dc/networks)
                          (dc/service-tasks service-id)))

(defn service-tasks
  [service-id]
  (->> (dmi/->tasks (dc/service-tasks service-id)
                    (dc/nodes)
                    (dc/services)
                    (dc/info))
       (map #(task-stats %))))

(defn service-tasks-id
  [service-id]
  (->> (dc/service-tasks service-id)
       (map :ID)))

(defn service-agent-logs
  "Fetch service log via swarmpit agent.

   Check whether there is agent associated with service task unless
   static configuration (:agent-url) set and fetch logs accordingly."
  [service-id since agent-tasks]
  (let [config-agent-url (cfg/config :agent-url)
        agent-addresses (dmi/->agent-addresses-by-nodes agent-tasks)
        service-tasks (dc/service-tasks service-id)
        service-containers (dmi/->service-tasks-by-container service-tasks)]
    (->> service-containers
         (pmap (fn [[k v]]
                 (let [dynamic-agent-url (get agent-addresses (:node v))
                       agent-url (or config-agent-url dynamic-agent-url)]
                   (when agent-url
                     (-> (sac/logs agent-url k since)
                         (dl/format-log (:task v)))))))
         (flatten)
         (dl/parse-agent-log))))

(defn service-native-logs
  "Fetch service log via native service api."
  [service-id since]
  (let [valid-since (if (or (time/valid? since)
                            (nil? since))
                      since
                      (time/to-unix-past
                        (time/since-to-minutes since)))]
    (-> (dc/service-logs service-id valid-since)
        (dl/parse-service-log)
        (or '()))))

(defn service-logs
  "Fetch logs via agent by default.

   In case of:
   * missing agent configuration (swarmpit.agent) label
   * no available agents
   * unexpected agent error

   fallback to native service api"
  [service-id since]
  (let [agent-tasks (dc/service-tasks-by-label :swarmpit.agent true)]
    (if (empty? agent-tasks)
      (service-native-logs service-id since)
      (try
        (service-agent-logs service-id since agent-tasks)
        (catch Exception _
          (service-native-logs service-id since))))))

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
        (du/dockerhub? repository-name) (dockerhub-by-namespace owner distribution-id)
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
      (assoc-in [:Labels] (:Labels service-delta))
      (assoc-in [:TaskTemplate :LogDriver :Options] (get-in service-delta [:TaskTemplate :LogDriver :Options]))))

(defn update-service
  [owner service-id service]
  (let [standardized-service (standardize-service owner service)
        service-origin (-> (dc/service service-id) :Spec)
        service-delta (dmo/->service standardized-service)]
    (dc/update-service (service-auth owner service)
                       service-id
                       (:version service)
                       (merge-service service-origin service-delta))))

(defn redeploy-service
  [owner service-id new-tag]
  (let [service-origin (dc/service service-id)
        service (dmi/->service service-origin)
        repository-name (get-in service [:repository :name])
        repository-tag (get-in service [:repository :tag])
        image-digest (repository-digest owner repository-name (or new-tag repository-tag))
        image (str repository-name ":" (or new-tag repository-tag) "@" image-digest)]
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

(defn stop-service
  [owner service-id]
  (let [service-origin (dc/service service-id)
        service (dmi/->service service-origin)]
    (when (= "replicated" (:mode service))
      (dc/update-service
        (service-auth owner service)
        service-id
        (get-in service-origin [:Version :Index])
        (-> service-origin
            :Spec
            (assoc-in [:Mode :Replicated :Replicas] 0))))))

;;; Node API

(defn node-stats
  [node]
  (if (= "down" (:state node))
    (assoc node :stats nil)
    (let [stats (apply dissoc (stats/node (:id node)) [:id :tasks])]
      (assoc node :stats stats))))

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

(defn delete-node
  [node-id]
  (dc/delete-node node-id))

(defn node-tasks
  [node-id]
  (->> (dmi/->tasks (dc/node-tasks node-id)
                    (dc/nodes)
                    (dc/services)
                    (dc/info))
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

(defn stack-tasks
  [stack-name]
  (let [stack-services (stack-services stack-name)]
    (->> (map #(service-tasks (:id %)) stack-services)
         (flatten)
         (filter #(= "running" (:state %))))))

(defn stack
  ([stack-name services]
   (when (not-empty services)
     {:stackName stack-name
      :stackFile (some? (stackfile stack-name))
      :services  services
      :networks  (networks-by-services services)
      :volumes   (volumes-by-services services)
      :configs   (resources-by-services services :configs configs)
      :secrets   (resources-by-services services :secrets secrets)}))
  ([stack-name]
   (stack stack-name (stack-services stack-name))))

(defn stack-compose
  [stack-name]
  (let [yaml (some-> (stack stack-name) (->compose) (->yaml))]
    (when yaml
      (str/replace yaml "$" "$$"))))

(defn service-compose
  [service-name]
  (some->> [(service service-name)]
           (stack nil)
           (->compose)
           (->yaml)))

(defn deployed-stacks
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
                     :state     "deployed"
                     :services  stack-services
                     :networks  (distinct-resources stack-networks)
                     :volumes   (distinct-resources stack-volumes)
                     :configs   (distinct-resources stack-configs)
                     :secrets   (distinct-resources stack-secrets)})))))))

(defn inactive-stacks
  [active-stacks]
  (let [active-stacks (into #{} (map :stackName active-stacks))]
    (->> (filter #(not (contains? active-stacks (:name %))) (cc/stackfiles))
         (map (fn [{:keys [name]}]
                (hash-map :stackName name
                          :state "inactive"))))))

(defn stacks
  []
  (let [deployed (deployed-stacks)]
    (concat deployed
            (inactive-stacks deployed))))

(defn stack-login
  [owner stackfile-spec]
  (let [distributions (remove nil?
                              (conj (registries-by-stackfile owner stackfile-spec)
                                    (dockerhub-by-stackfile owner stackfile-spec)))]
    (doseq [{:keys [type username password url] :as distro} distributions]
      (case type
        "ecr" (dcli/login "AWS" (registry-ecr-password distro) url)
        "acr" (dcli/login (:spId distro) (:spPassword distro) url)
        "gitlab" (dcli/login (:username distro) (:token distro) url)
        (dcli/login username password url)))))

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
