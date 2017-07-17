(ns swarmpit.docker.mapper.inbound
  "Map docker domain to swarmpit domain"
  (:import (java.text SimpleDateFormat))
  (:require [clojure.string :as str]))

(def date-format (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss"))

(defn date
  [date]
  (str (.parse date-format date)))

(defn ->node
  [node]
  (array-map
    :id (get node :ID)
    :nodeName (get-in node [:Description :Hostname])
    :role (get-in node [:Spec :Role])
    :availability (get-in node [:Spec :Availability])
    :labels (get-in node [:Spec :Labels])
    :state (get-in node [:Status :State])
    :address (get-in node [:Status :Addr])
    :engine (get-in node [:Description :Engine :EngineVersion])
    :leader (get-in node [:ManagerStatus :Leader])))

(defn ->nodes
  [nodes]
  (->> nodes
       (map ->node)
       (into [])))

(defn ->task-node
  [task nodes]
  (let [task-node (first (filter #(= (:ID %)
                                     (:NodeID task)) nodes))]
    (->node task-node)))

(defn ->task
  [task nodes service-name service-mode]
  (let [image (get-in task [:Spec :ContainerSpec :Image])
        image-info (str/split image #"@")
        image-name (first image-info)
        image-digest (second image-info)
        slot (get task :Slot)
        id (get task :ID)
        task-name (if (= "replicated" service-mode)
                    (str service-name "." slot)
                    service-name)]
    (array-map
      :id id
      :taskName task-name
      :version (get-in task [:Version :Index])
      :createdAt (date (get task :CreatedAt))
      :updatedAt (date (get task :UpdatedAt))
      :repository {:image       image-name
                   :imageDigest image-digest}
      :state (get-in task [:Status :State])
      :status {:error (get-in task [:Status :Err])}
      :desiredState (get task :DesiredState)
      :serviceName service-name
      :node (->task-node task nodes))))

(defn ->tasks
  [tasks nodes service-name service-mode]
  (->> tasks
       (map #(->task % nodes service-name service-mode))
       (into [])))

(defn ->service-ports
  [service-spec]
  (->> (get-in service-spec [:EndpointSpec :Ports])
       (map (fn [p] {:containerPort (:TargetPort p)
                     :protocol      (:Protocol p)
                     :hostPort      (:PublishedPort p)}))
       (into [])))

(defn ->service-network
  [network networks]
  (first (filter #(= (:Id %)
                     (:Target network)) networks)))

(defn ->service-networks
  [service-spec networks]
  (->> (get-in service-spec [:TaskTemplate :Networks])
       (map (fn [n] (->service-network n networks)))
       (map (fn [n] {:networkName (:Name n)
                     :driver      (:Driver n)}))
       (into [])))

(defn ->service-mounts
  [service-spec]
  (->> (get-in service-spec [:TaskTemplate :ContainerSpec :Mounts])
       (map (fn [v] {:containerPath (:Target v)
                     :hostPath      (:Source v)
                     :type          (:Type v)
                     :readOnly      (contains? #{true 1} (:ReadOnly v))}))
       (into [])))

(defn ->service-variables
  [service-spec]
  (->> (get-in service-spec [:TaskTemplate :ContainerSpec :Env])
       (map (fn [p]
              (let [variable (str/split p #"=")]
                {:name  (first variable)
                 :value (second variable)})))
       (into [])))

(defn ->service-labels
  [service-labels]
  (->> service-labels
       (filter #(not (or (str/starts-with? (name (key %)) "swarmpit")
                         (str/starts-with? (name (key %)) "com.docker"))))
       (map (fn [l]
              {:name  (name (key l))
               :value (val l)}))
       (into [])))

(defn ->service-placement-constraints
  [service-spec]
  (->> (get-in service-spec [:TaskTemplate :Placement :Constraints])
       (map (fn [v] {:rule v}))
       (into [])))

(defn ->service-secrets
  [service-spec]
  (->> (get-in service-spec [:TaskTemplate :ContainerSpec :Secrets])
       (map (fn [s] {:secretName (:SecretName s)}))
       (into [])))

(defn ->service-deployment-update
  [service-spec]
  (let [update-config (:UpdateConfig service-spec)]
    {:parallelism   (or (:Parallelism update-config) 1)
     :delay         (/ (or (:Delay update-config) 0) 1000000000)
     :failureAction (or (:FailureAction update-config) "pause")}))

(defn ->service-deployment-rollback
  [service-spec]
  (let [update-config (:RollbackConfig service-spec)]
    {:parallelism   (or (:Parallelism update-config) 1)
     :delay         (/ (or (:Delay update-config) 0) 1000000000)
     :failureAction (or (:FailureAction update-config) "pause")}))

(defn ->service-deployment-restart-policy
  [service-task-template]
  (let [restart-policy (:RestartPolicy service-task-template)]
    {:condition (or (:Condition restart-policy) "any")
     :delay     (/ (or (:Delay restart-policy) 5000000000) 1000000000)
     :attempts  (or (:MaxAttempts restart-policy) 0)}))

(defn ->service-replicas-running
  [service-tasks]
  (-> (filter #(= (get-in % [:Status :State]) "running") service-tasks)
      (count)))

(defn ->service-info-status
  [service-replicas service-replicas-running service-mode]
  (if (= service-mode "replicated")
    (str service-replicas-running " / " service-replicas)
    (str service-replicas-running " / " service-replicas-running)))

(defn ->service-state
  [service-replicas service-replicas-running service-mode]
  (case service-mode
    "replicated" (if (zero? service-replicas-running)
                   "not running"
                   (if (= service-replicas-running service-replicas)
                     "running"
                     "partly running"))
    "global" (if (zero? service-replicas-running)
               "not running"
               "running")))

(defn ->service-autoredeploy
  [service-labels]
  (let [value (:swarmpit.service.deployment.autoredeploy service-labels)]
    (if (some? value)
      (= "true" value)
      value)))

(defn ->service
  [service tasks nodes networks]
  (let [service-spec (:Spec service)
        service-labels (:Labels service-spec)
        service-task-template (:TaskTemplate service-spec)
        service-mode (str/lower-case (name (first (keys (:Mode service-spec)))))
        service-name (:Name service-spec)
        service-id (:ID service)
        service-tasks (filter #(= (:ServiceID %) service-id) tasks)
        replicas (get-in service-spec [:Mode :Replicated :Replicas])
        replicas-running (->service-replicas-running service-tasks)
        image (get-in service-task-template [:ContainerSpec :Image])
        image-info (str/split image #"@")
        image-name (first image-info)
        image-digest (second image-info)
        image-segments (str/split image-name #":")]
    (array-map
      :id service-id
      :version (get-in service [:Version :Index])
      :createdAt (date (:CreatedAt service))
      :updatedAt (date (:UpdatedAt service))
      :registry {:name (:swarmpit.service.registry.name service-labels)
                 :user (:swarmpit.service.registry.user service-labels)}
      :repository {:image       image-name
                   :imageDigest image-digest
                   :name        (first image-segments)
                   :tag         (second image-segments)}
      :serviceName service-name
      :mode service-mode
      :replicas replicas
      :state (->service-state replicas replicas-running service-mode)
      :status {:info    (->service-info-status replicas replicas-running service-mode)
               :update  (get-in service [:UpdateStatus :State])
               :message (get-in service [:UpdateStatus :Message])}
      :ports (->service-ports service-spec)
      :networks (->service-networks service-spec networks)
      :mounts (->service-mounts service-spec)
      :secrets (->service-secrets service-spec)
      :variables (->service-variables service-spec)
      :labels (->service-labels service-labels)
      :deployment {:update        (->service-deployment-update service-spec)
                   :forceUpdate   (:ForceUpdate service-task-template)
                   :restartPolicy (->service-deployment-restart-policy service-task-template)
                   :rollback      (->service-deployment-rollback service-spec)
                   :autoredeploy  (->service-autoredeploy service-labels)
                   :placement     (->service-placement-constraints service-spec)}
      :tasks (->tasks service-tasks nodes service-name service-mode))))

(defn ->services
  [services tasks nodes networks]
  (->> services
       (map #(->service % tasks nodes networks))
       (into [])))

(defn ->network
  [network]
  (let [config (first (get-in network [:IPAM :Config]))]
    (array-map
      :id (get network :Id)
      :networkName (get network :Name)
      :created (date (get network :Created))
      :scope (get network :Scope)
      :driver (get network :Driver)
      :internal (get network :Internal)
      :ipam {:subnet  (:Subnet config)
             :gateway (:Gateway config)})))

(defn ->networks
  [networks]
  (->> networks
       (map ->network)
       (filter #(= "swarm" (:scope %)))
       (into [])))

(defn ->volume
  [volume]
  (let [name (get volume :Name)]
    (array-map
      :id (hash name)
      :volumeName name
      :driver (get volume :Driver)
      :mountpoint (get volume :Mountpoint)
      :scope (get volume :Scope))))

(defn ->volumes
  [volumes]
  (->> (:Volumes volumes)
       (map ->volume)
       (into [])))

(defn ->secret
  [secret]
  (array-map
    :id (get secret :ID)
    :version (get-in secret [:Version :Index])
    :secretName (get-in secret [:Spec :Name])
    :createdAt (date (get secret :CreatedAt))
    :updatedAt (date (get secret :UpdatedAt))))

(defn ->secrets
  [secrets]
  (->> secrets
       (map ->secret)
       (into [])))