(ns swarmpit.api
  (:require [clojure.core.memoize :as memo]
            [clojure.set :refer [rename-keys]]
            [buddy.hashers :as hashers]
            [digest :refer [digest]]
            [swarmpit.docker.client :as dc]
            [swarmpit.docker.mapper.inbound :as dmi]
            [swarmpit.docker.mapper.outbound :as dmo]
            [swarmpit.dockerauth.client :as dac]
            [swarmpit.dockerregistry.client :as drc]
            [swarmpit.dockerhub.client :as dhc]
            [swarmpit.dockerhub.mapper.inbound :as dhmi]
            [swarmpit.registry.client :as rc]
            [swarmpit.registry.mapper.inbound :as rmi]
            [swarmpit.couchdb.client :as cc]
            [swarmpit.couchdb.mapper.inbound :as cmi]
            [swarmpit.couchdb.mapper.outbound :as cmo]))

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

;;; Secret API

(defn secrets
  []
  (-> (dc/secrets)
      (dmi/->secrets)))

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

;;; Network API

(defn networks
  []
  (-> (dc/networks)
      (dmi/->networks)))

(defn network
  [network-id]
  (-> (dc/network network-id)
      (dmi/->network)))

(defn delete-network
  [network-id]
  (dc/delete-network network-id))

(defn create-network
  [network]
  (->> (dmo/->network network)
       (dc/create-network)
       ((fn [net] {:id (:Id net)}))))

;;; Volume API

(defn volumes
  []
  (-> (dc/volumes)
      (dmi/->volumes)))

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

;;; Node API

(defn nodes
  []
  (-> (dc/nodes)
      (dmi/->nodes)))

(defn node
  [node-id]
  (-> (dc/node node-id)
      (dmi/->node)))

;;; Dockerhub API

(defn dockerusers
  [owner]
  (-> (cc/dockerusers owner)
      (cmi/->dockerusers)))

(defn dockeruser-info
  [dockeruser]
  (dhc/info dockeruser))

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

(defn create-dockeruser
  [dockeruser dockeruser-info]
  (if (not (dockeruser-exist? dockeruser))
    (->> (cmo/->docker-user dockeruser dockeruser-info)
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
        user-token (:token (dhc/login dockeruser))]
    (->> (dhc/namespaces user-token)
         :namespaces
         (map #(:results (dhc/repositories-by-namespace user-token %)))
         (flatten)
         (dhmi/->user-repositories)
         (filter #(:private %)))))

(defn dockeruser-tags
  [dockeruser-id repository-name]
  (let [user (cc/dockeruser dockeruser-id)
        token (:token (dac/token user repository-name))]
    (->> (drc/tags token repository-name)
         :tags)))

(defn dockeruser-latest-distribution
  [{:keys [id] :as distribution}
   {:keys [name tag] :as repository}]
  (-> (cc/dockeruser id)
      (dac/token name)
      :token
      (drc/distribution name tag)))

(defn dockeruser-ports
  [dockeruser-id repository-name repository-tag]
  (let [user (cc/dockeruser dockeruser-id)
        token (:token (dac/token user repository-name))]
    (-> (drc/manifest token repository-name repository-tag)
        (rmi/->repository-config)
        :config
        (dmi/->image-ports))))

;; Public dockerhub API

(defn public-repositories
  [repository-query repository-page]
  (-> (dhc/repositories repository-query repository-page)
      (dhmi/->repositories repository-query repository-page)))

(defn public-tags
  [repository-name]
  (dockeruser-tags nil repository-name))

(defn public-latest-distribution
  [repository]
  (dockeruser-latest-distribution nil repository))

(defn public-ports
  [repository-name repository-tag]
  (dockeruser-ports nil repository-name repository-tag))

;;; Registry API

(defn registries
  [owner]
  (-> (cc/registries owner)
      (cmi/->registries)))

(defn registry
  [registry-id]
  (-> (cc/registry registry-id)
      (cmi/->registry)))

(defn registry-exist?
  [registry]
  (cc/registry-exist? registry))

(defn registry-valid?
  [registry]
  (some?
    (try
      (rc/info registry)
      (catch Exception _))))

(defn delete-registry
  [registry-id]
  (->> (registry registry-id)
       (cc/delete-registry)))

(defn create-registry
  [registry]
  (if (not (registry-exist? registry))
    (->> (cmo/->registry registry)
         (cc/create-registry))))

(defn update-registry
  [registry-id registry-delta]
  (->> (cc/update-registry (cc/registry registry-id) registry-delta)
       (cmi/->registry)))

(defn registry-repositories
  [registry-id]
  (->> (cc/registry registry-id)
       (rc/repositories)
       (rmi/->repositories)))

(defn registry-tags
  [registry-id repository-name]
  (-> (cc/registry registry-id)
      (rc/tags repository-name)
      :tags))

(defn registry-latest-distribution
  [{:keys [id] :as distribution}
   {:keys [name tag] :as repository}]
  (-> (cc/registry id)
      (rc/distribution name tag)))

(defn registry-ports
  [registry-id repository-name repository-tag]
  (-> (cc/registry registry-id)
      (rc/manifest repository-name repository-tag)
      (rmi/->repository-config)
      :config
      (dmi/->image-ports)))

;;; Image API

(defn image
  [img]
  (dc/image img))

(defn image-ports
  [img]
  (-> (image img)
      :Config
      (dmi/->image-ports)))

;;; Service API

(defn services
  []
  (dmi/->services (dc/services)
                  (dc/tasks)))

(defn service
  [service-id]
  (dmi/->service (dc/service service-id)
                 (dc/service-tasks service-id)))

(defn service-networks
  [service-id]
  (dmi/->service-networks (dc/service service-id)
                          (dc/networks)))

(defn service-tasks
  [service-id]
  (dmi/->tasks (dc/service-tasks service-id)
               (dc/nodes)
               (dc/services)))

(defn delete-service
  [service-id]
  (dc/delete-service service-id))

(defn create-public-service
  [service secrets]
  (->> (dmo/->service-image service)
       (dmo/->service service secrets)
       (dc/create-service)))

(defn create-dockerhub-service
  [service dockeruser-id secrets]
  (let [auth-config (->> (cc/dockeruser dockeruser-id)
                         (dmo/->auth-config))]
    (->> (dmo/->service-image service)
         (dmo/->service service secrets)
         (dc/create-service auth-config))))

(defn create-registry-service
  [service registry-id secrets]
  (let [registry (cc/registry registry-id)
        auth-config (dmo/->auth-config registry)]
    (->> (dmo/->service-image-registry service registry)
         (dmo/->service service secrets)
         (dc/create-service auth-config))))

(defn create-service
  [service]
  (let [distribution-id (get-in service [:distribution :id])
        distribution-type (get-in service [:distribution :type])
        secrets (dmo/->service-secrets service (secrets))]
    (rename-keys
      (case distribution-type
        "dockerhub" (create-dockerhub-service service distribution-id secrets)
        "registry" (create-registry-service service distribution-id secrets)
        (create-public-service service secrets)) {:ID :id})))

(defn update-service
  [service-id service force?]
  (let [secrets (dmo/->service-secrets service (secrets))
        service-payload (->> (dmo/->service-image service)
                             (dmo/->service service secrets))]
    (dc/update-service service-id
                       (:version service)
                       (update-in service-payload [:TaskTemplate :ForceUpdate]
                                  (if force?
                                    inc
                                    identity)))))

(defn service-image-id
  [service-repository]
  (-> (image (:image service-repository))
      :Id))

(defn service-image-latest-id
  [{:keys [type] :as service-distribution} service-repository]
  (get-in
    (case type
      "dockerhub" (dockeruser-latest-distribution service-distribution service-repository)
      "registry" (registry-latest-distribution service-distribution service-repository)
      (public-latest-distribution service-repository)) [:config :digest]))

;;; Task API

(defn tasks
  []
  (dmi/->tasks (dc/tasks)
               (dc/nodes)
               (dc/services)))

(defn task
  [task-id]
  (dmi/->task (dc/task task-id)
              (dc/nodes)
              (dc/services)))

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
        nodes-label (->> (map :labels nodes)
                         (into {}))]
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
          (str "node.labels." (name (key item)) matcher (val item)))))))