(ns swarmpit.docker.engine.mapper.inbound
  "Map docker domain to swarmpit domain"
  (:require [clojure.string :as str]
            [swarmpit.utils :refer [map->name-value nano->]]))

(defn- as-megabytes
  [bytes]
  (quot bytes (* 1024 1024)))

(defn ->resources
  [resources]
  (let [nano-cpu (:NanoCPUs resources)
        memory-bytes (:MemoryBytes resources)]
    {:cpu    (-> (or nano-cpu 0)
                 (nano->)
                 (double))
     :memory (-> (or memory-bytes 0)
                 (as-megabytes))}))

(def stack-label :com.docker.stack.namespace)
(def autoredeploy-label :swarmpit.service.deployment.autoredeploy)

(defn ->image-ports
  [image-config]
  (let [ports (:ExposedPorts image-config)]
    (->> (keys ports)
         (map #(let [port-segment (str/split (str %) #"/")]
                 {:containerPort (Integer. (subs (first port-segment) 1))
                  :protocol      (second port-segment)
                  :hostPort      0}))
         (into []))))

(defn ->network
  [network]
  (let [config (first (get-in network [:IPAM :Config]))]
    (array-map
      :id (:Id network)
      :networkName (:Name network)
      :created (:Created network)
      :scope (:Scope network)
      :driver (:Driver network)
      :internal (:Internal network)
      :options (map->name-value (:Options network))
      :attachable (:Attachable network)
      :ingress (:Ingress network)
      :enableIPv6 (:EnableIPv6 network)
      :labels (:Labels network)
      :stack (-> network :Labels stack-label)
      :ipam {:subnet  (:Subnet config)
             :gateway (:Gateway config)})))

(defn ->networks
  [networks]
  (->> networks
       (map ->network)
       (filter #(not (contains? #{"null" "host"} (:driver %))))
       (into [])))

(defn ->plugins
  [node]
  (let [m (->> node
               :Description
               :Engine
               :Plugins
               (group-by :Type))]
    {:networks (->> (get m "Network")
                    (map :Name))
     :volumes  (->> (get m "Volume")
                    (map :Name))}))

(defn ->node
  [node]
  (array-map
    :id (:ID node)
    :version (get-in node [:Version :Index])
    :nodeName (get-in node [:Description :Hostname])
    :role (get-in node [:Spec :Role])
    :availability (get-in node [:Spec :Availability])
    :labels (map->name-value (get-in node [:Spec :Labels]))
    :state (get-in node [:Status :State])
    :address (get-in node [:Status :Addr])
    :engine (get-in node [:Description :Engine :EngineVersion])
    :arch (get-in node [:Description :Platform :Architecture])
    :os (get-in node [:Description :Platform :OS])
    :resources (->resources (get-in node [:Description :Resources]))
    :plugins (->plugins node)
    :leader (get-in node [:ManagerStatus :Leader])))

(defn ->nodes
  [nodes]
  (->> nodes
       (map ->node)
       (into [])))

(defn- ->service-mode
  [service-spec]
  (when service-spec
    (str/lower-case (name (first (keys (:Mode service-spec)))))))

(defn ->task-node
  [node-id nodes]
  (first (filter #(= (:ID %) node-id) nodes)))

(defn ->task-service
  [service-id services]
  (first (filter #(= (:ID %) service-id) services)))

(defn ->task-name
  "If there is no matching service for task use task-id as name. Otherwise follow rules"
  [task-id node-id slot service-name service-mode]
  (cond
    (= "replicated" service-mode) (str service-name "." slot)
    (= "global" service-mode) (str service-name "." node-id)
    :else task-id))

(defn ->task
  [task nodes services]
  (let [image (get-in task [:Spec :ContainerSpec :Image])
        image-info (str/split image #"@")
        image-name (first image-info)
        image-digest (second image-info)
        slot (:Slot task)
        id (:ID task)
        node-id (:NodeID task)
        node (->task-node node-id nodes)
        node-name (get-in node [:Description :Hostname])
        service-id (:ServiceID task)
        service (->task-service service-id services)
        service-name (get-in service [:Spec :Name])
        service-mode (->service-mode (:Spec service))
        task-name (->task-name id node-id slot service-name service-mode)]
    (array-map
      :id id
      :taskName task-name
      :version (get-in task [:Version :Index])
      :createdAt (:CreatedAt task)
      :updatedAt (:UpdatedAt task)
      :repository {:image       image-name
                   :imageDigest image-digest}
      :state (get-in task [:Status :State])
      :status {:error (get-in task [:Status :Err])}
      :desiredState (:DesiredState task)
      :serviceName (or service-name service-id)
      :nodeId node-id
      :nodeName (or node-name node-id))))

(defn ->tasks
  [tasks nodes services]
  (->> tasks
       (map #(->task % nodes services))
       (into [])))

(defn ->service-tasks
  [service-id tasks]
  (filter #(= (:ServiceID %) service-id) tasks))

(defn ->service-ports
  [service]
  (->> (get-in service [:Endpoint :Ports])
       (map (fn [p] {:containerPort (:TargetPort p)
                     :protocol      (:Protocol p)
                     :mode          (:PublishMode p)
                     :hostPort      (:PublishedPort p)}))
       (into [])))

(defn ->service-networks
  [service networks]
  (let [networks (group-by :Id networks)]
    (->> (get-in service [:Spec :TaskTemplate :Networks])
         (map #(->> (get networks (:Target %))
                    (first)
                    (->network)
                    (merge {:serviceAliases (:Aliases %)})))
         (into []))))

(defn ->service-mount-options
  [volume-options]
  (when volume-options
    {:labels (-> volume-options :Labels)
     :driver {:name    (-> volume-options :DriverConfig :Name)
              :options (-> volume-options :DriverConfig :Options)}}))

(defn ->service-mounts
  [service-spec]
  (->> (get-in service-spec [:TaskTemplate :ContainerSpec :Mounts])
       (map (fn [v] {:containerPath (:Target v)
                     :host          (:Source v)
                     :type          (:Type v)
                     :id            (when (= "volume" (:Type v)) (:Source v))
                     :volumeOptions (->service-mount-options (:VolumeOptions v))
                     :readOnly      (contains? #{true 1} (:ReadOnly v))
                     :stack         (-> v :VolumeOptions :Labels stack-label)}))
       (into [])))

(defn ->service-variables
  [service-spec]
  (->> (get-in service-spec [:TaskTemplate :ContainerSpec :Env])
       (map (fn [p]
              (let [variable (str/split p #"=" 2)]
                {:name  (first variable)
                 :value (second variable)})))
       (into [])))

(defn ->service-labels
  [service-labels]
  (->> service-labels
       (filter #(not (or (str/starts-with? (name (key %)) "swarmpit")
                         (str/starts-with? (name (key %)) "com.docker"))))
       (map->name-value)))

(defn ->service-log-options
  [service-task-template]
  (let [log-driver (get-in service-task-template [:LogDriver :Options])]
    (->> log-driver
         (map->name-value))))

(defn ->service-placement-constraints
  [service-spec]
  (->> (get-in service-spec [:TaskTemplate :Placement :Constraints])
       (map (fn [v] {:rule v}))
       (into [])))

(defn ->service-secrets
  [service-spec]
  (->> (get-in service-spec [:TaskTemplate :ContainerSpec :Secrets])
       (map (fn [s] {:id           (:SecretID s)
                     :secretName   (:SecretName s)
                     :secretTarget (get-in s [:File :Name])
                     :uid          (get-in s [:File :UID])
                     :gid          (get-in s [:File :GID])
                     :mode         (get-in s [:File :Mode])}))
       (into [])))

(defn ->service-configs
  [service-spec]
  (->> (get-in service-spec [:TaskTemplate :ContainerSpec :Configs])
       (map (fn [s] {:id           (:ConfigID s)
                     :configName   (:ConfigName s)
                     :configTarget (get-in s [:File :Name])
                     :uid          (get-in s [:File :UID])
                     :gid          (get-in s [:File :GID])
                     :mode         (get-in s [:File :Mode])}))
       (into [])))

(defn ->service-deployment-update
  [service-spec]
  (let [update-config (:UpdateConfig service-spec)]
    {:parallelism   (or (:Parallelism update-config) 1)
     :delay         (nano-> (or (:Delay update-config) 0))
     :order         (or (:Order update-config) "stop-first")
     :failureAction (or (:FailureAction update-config) "pause")}))

(defn ->service-deployment-rollback
  [service-spec]
  (let [update-config (:RollbackConfig service-spec)]
    {:parallelism   (or (:Parallelism update-config) 1)
     :delay         (nano-> (or (:Delay update-config) 0))
     :order         (or (:Order update-config) "stop-first")
     :failureAction (or (:FailureAction update-config) "pause")}))

(defn ->service-deployment-restart-policy
  [service-task-template]
  (let [restart-policy (:RestartPolicy service-task-template)]
    {:condition (or (:Condition restart-policy) "any")
     :delay     (nano-> (or (:Delay restart-policy) 5000000000))
     :window    (nano-> (or (:Window restart-policy) 0))
     :attempts  (or (:MaxAttempts restart-policy) 0)}))

(defn ->service-replicas-running
  [service-tasks]
  (-> (filter #(and (= (get-in % [:Status :State]) "running")
                    (= (:DesiredState %) "running")) service-tasks)
      (count)))

(defn ->service-replicas-no-shutdown
  [service-tasks]
  (-> (filter #(not (= (:DesiredState %) "shutdown")) service-tasks)
      (count)))

(defn ->service-info-status
  [service-replicas-running service-replicas]
  (str service-replicas-running " / " service-replicas))

(defn ->service-state
  [service-replicas-running service-replicas]
  (if (zero? service-replicas-running)
    "not running"
    (if (= service-replicas-running service-replicas)
      "running"
      "partly running")))

(defn ->service-autoredeploy
  [service-labels]
  (let [value (autoredeploy-label service-labels)]
    (= "true" value)))

(defn ->service-image-details
  [image-name]
  (when (some? image-name)
    (let [separator-pos (str/last-index-of image-name ":")
          length (count image-name)]
      (if (some? separator-pos)
        {:name (subs image-name 0 separator-pos)
         :tag  (subs image-name (inc separator-pos) length)}
        {:name (subs image-name 0 length)
         :tag  ""}))))

(defn ->service
  ([service]
   (->service service nil nil))
  ([service tasks networks]
   (let [service-spec (:Spec service)
         service-labels (:Labels service-spec)
         service-task-template (:TaskTemplate service-spec)
         service-mode (->service-mode service-spec)
         service-name (:Name service-spec)
         service-id (:ID service)
         service-tasks (->service-tasks service-id tasks)
         replicas (get-in service-spec [:Mode :Replicated :Replicas])
         replicas-running (->service-replicas-running service-tasks)
         replicas-no-shutdown (->service-replicas-no-shutdown service-tasks)
         image (get-in service-task-template [:ContainerSpec :Image])
         image-info (str/split image #"@")
         image-name (first image-info)
         image-digest (second image-info)]
     (array-map
       :id service-id
       :version (get-in service [:Version :Index])
       :createdAt (:CreatedAt service)
       :updatedAt (:UpdatedAt service)
       :repository (merge (->service-image-details image-name)
                          {:image       image-name
                           :imageDigest image-digest})
       :serviceName service-name
       :mode service-mode
       :stack (-> service-labels stack-label)
       :replicas replicas
       :state (if (= service-mode "replicated")
                (->service-state replicas-running replicas)
                (->service-state replicas-running replicas-no-shutdown))
       :status {:tasks   {:running replicas-running
                          :total   (if (= service-mode "replicated")
                                     replicas
                                     replicas-no-shutdown)}
                :update  (get-in service [:UpdateStatus :State])
                :message (get-in service [:UpdateStatus :Message])}
       :ports (->service-ports service)
       :mounts (->service-mounts service-spec)
       :networks (->service-networks service networks)
       :secrets (->service-secrets service-spec)
       :configs (->service-configs service-spec)
       :variables (->service-variables service-spec)
       :labels (->service-labels service-labels)
       :command (get-in service-task-template [:ContainerSpec :Args])
       :logdriver {:name (or (get-in service-task-template [:LogDriver :Name]) "json-file")
                   :opts (->service-log-options service-task-template)}
       :resources {:reservation (->resources (get-in service-task-template [:Resources :Reservations]))
                   :limit       (->resources (get-in service-task-template [:Resources :Limits]))}
       :deployment {:update          (->service-deployment-update service-spec)
                    :forceUpdate     (:ForceUpdate service-task-template)
                    :restartPolicy   (->service-deployment-restart-policy service-task-template)
                    :rollback        (->service-deployment-rollback service-spec)
                    :rollbackAllowed (some? (:PreviousSpec service))
                    :autoredeploy    (->service-autoredeploy service-labels)
                    :placement       (->service-placement-constraints service-spec)}))))

(defn ->services
  [services tasks networks]
  (->> services
       (map #(->service % tasks networks))
       (into [])))

(defn ->volume
  [volume]
  (let [name (:Name volume)]
    (array-map
      :id name
      :volumeName name
      :driver (:Driver volume)
      :stack (-> volume :Labels stack-label)
      :labels (:Labels volume)
      :options (map->name-value (:Options volume))
      :mountpoint (:Mountpoint volume)
      :scope (:Scope volume))))

(defn ->volumes
  [volumes]
  (->> (:Volumes volumes)
       (map ->volume)
       (into [])))

(defn ->secret
  [secret]
  (array-map
    :id (:ID secret)
    :version (get-in secret [:Version :Index])
    :secretName (get-in secret [:Spec :Name])
    :createdAt (:CreatedAt secret)
    :updatedAt (:UpdatedAt secret)))

(defn ->secrets
  [secrets]
  (->> secrets
       (map ->secret)
       (into [])))

(defn ->config
  [config]
  (array-map
    :id (:ID config)
    :version (get-in config [:Version :Index])
    :configName (get-in config [:Spec :Name])
    :createdAt (:CreatedAt config)
    :updatedAt (:UpdatedAt config)
    :data (get-in config [:Spec :Data])))

(defn ->configs
  [configs]
  (->> configs
       (map ->config)
       (into [])))