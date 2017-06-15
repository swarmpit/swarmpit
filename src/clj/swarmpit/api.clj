(ns swarmpit.api
  (:require [clojure.core.memoize :as memo]
            [clojure.string :as string]
            [swarmpit.docker.client :as dc]
            [swarmpit.docker.mapper.inbound :as dmi]
            [swarmpit.docker.mapper.outbound :as dmo]
            [swarmpit.dockerhub.client :as dhc]
            [swarmpit.dockerhub.mapper.inbound :as dhmi]
            [swarmpit.registry.client :as rc]
            [swarmpit.registry.mapper.inbound :as rmi]
            [swarmpit.couchdb.client :as cc]
            [swarmpit.couchdb.mapper.outbound :as cmo]))

;;; User API

(defn users
  []
  (cc/users))

(defn user
  [user-id]
  (cc/user user-id))

(defn user-by-credentials
  [credentails]
  (cc/user-by-credentials (:username credentails)
                          (cmo/->password (:password credentails))))

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

;;; Registry API

(defn registries
  []
  (cc/registries))

(defn registries-sum
  []
  (->> (registries)
       (map :name)
       (into [])))

(defn registry
  [registry-id]
  (cc/registry registry-id))

(defn registry-by-name
  [registry-name]
  (cc/registry-by-name registry-name))

(defn registry-exist?
  [registry]
  (some? (registry-by-name (:name registry))))

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

;;; Service API

(defn services
  []
  (dmi/->services (dc/services)
                  (dc/tasks)
                  (dc/nodes)
                  (dc/networks)))

(defn service
  [service-id]
  (dmi/->service (dc/service service-id)
                 (dc/tasks)
                 (dc/nodes)
                 (dc/networks)))

(defn delete-service
  [service-id]
  (dc/delete-service service-id))

(defn create-service
  [service]
  (let [registry-name (get-in service [:repository :registry])]
    (if (= "dockerhub" registry-name)
      (->> (dmo/->service-image service)
           (dmo/->service service)
           (dc/create-service))
      (let [registry (registry-by-name registry-name)
            auth-config (dmo/->auth-config registry)]
        (->> (dmo/->service-image-registry service registry)
             (dmo/->service service)
             (dc/create-service auth-config))))))

(defn update-service
  [service-id service]
  (let [service-version (:version service)]
    (->> (dmo/->service-image service)
         (dmo/->service service)
         (dc/update-service service-id service-version))))

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
       (dc/create-network)))

;;; Node API

(defn nodes
  []
  (-> (dc/nodes)
      (dmi/->nodes)))

(defn node
  [node-id]
  (-> (dc/node node-id)
      (dmi/->node)))

;;; Task API

(defn tasks
  []
  (->> (services)
       (map #(:tasks %))
       (flatten)))

(defn task
  [task-id]
  (->> (tasks)
       (filter #(= (:id %) task-id))
       (first)))

;;; Repository Dockerhub API

(defn dockerusers
  []
  (cc/docker-users))

(defn dockerusers-sum
  []
  (->> (dockerusers)
       (map :username)
       (into [])))

(defn dockeruser-info
  [dockeruser]
  (dhc/info dockeruser))

(defn dockeruser-login
  [dockeruser]
  (dhc/login dockeruser))

(defn dockeruser-by-username
  [docker-username]
  (cc/docker-user-by-name docker-username))

(defn dockeruser
  [dockeruser-id]
  (cc/docker-user dockeruser-id))

(defn create-dockeruser
  [dockeruser dockeruser-info]
  (->> (cmo/->docker-user dockeruser dockeruser-info)
       (cc/create-docker-user)))

(defn delete-dockeruser
  [dockeruser-id]
  (-> (dockeruser dockeruser-id)
      (cc/delete-docker-user)))

(defn dockeruser-repositories
  [dockeruser]
  (let [user-token (:token (dhc/login dockeruser))]
    (->> (dhc/namespaces user-token)
         :namespaces
         (map #(:results (dhc/repositories-by-namespace user-token %)))
         (flatten)
         (dhmi/->user-repositories)
         (filter #(:private %)))))

(defn dockerhub-repositories
  [repository-query repository-page]
  (-> (dhc/repositories repository-query repository-page)
      (dhmi/->repositories repository-query repository-page)))

(defn dockerhub-tags
  [repository-name username]
  (let [user (dockeruser-by-username username)]
    (->> (dhc/tags repository-name user)
         (map :name)
         (into []))))

;;; Repository V2 API

(defn repositories
  [registry]
  (->> (rc/repositories registry)
       (rmi/->repositories)))

(defn tags
  [registry repository-name]
  (->> (rc/tags registry repository-name)
       :tags))