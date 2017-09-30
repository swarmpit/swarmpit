(ns swarmpit.docker.mapper.inbound
  "Map docker domain to swarmpit domain"
  (:require [clojure.string :as str]))

(defn- as-megabytes
  [bytes]
  (quot bytes (* 1024 1024)))

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
      :ipam {:subnet  (:Subnet config)
             :gateway (:Gateway config)})))

(defn ->networks
  [networks]
  (->> networks
       (map ->network)
       (filter #(not (contains? #{"null" "host"} (:driver %))))
       (into [])))

(defn ->node
  [node]
  (array-map
    :id (:ID node)
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

(defn- ->service-mode
  [service-spec]
  (str/lower-case (name (first (keys (:Mode service-spec))))))

(defn ->task-node
  [node-id nodes]
  (or (first (filter #(= (:ID %) node-id) nodes)) node-id))

(defn ->task-service
  [service-id services]
  (first (filter #(= (:ID %) service-id) services)))

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
        task-name (if (= "replicated" service-mode)
                    (str service-name "." slot)
                    service-name)]
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
      :serviceName service-name
      :nodeName node-name)))

(defn ->tasks
  [tasks nodes services]
  (->> tasks
       (map #(->task % nodes services))
       (into [])))

(defn ->service-tasks
  [service-id tasks]
  (filter #(= (:ServiceID %) service-id) tasks))

(defn ->service-ports
  [service-spec]
  (->> (get-in service-spec [:EndpointSpec :Ports])
       (map (fn [p] {:containerPort (:TargetPort p)
                     :protocol      (:Protocol p)
                     :hostPort      (:PublishedPort p)}))
       (into [])))

(defn ->service-network
  [network-id networks]
  (first (filter #(= (:Id %)
                     network-id) networks)))

(defn ->service-networks
  [service networks]
  (->> (get-in service [:Spec :TaskTemplate :Networks])
       (map (fn [n] (->service-network (:Target n) networks)))
       (map (fn [n] (->network n)))
       (into [])))

(defn ->service-mounts
  [service-spec]
  (->> (get-in service-spec [:TaskTemplate :ContainerSpec :Mounts])
       (map (fn [v] {:containerPath (:Target v)
                     :host          (:Source v)
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
       (map (fn [l] {:name  (name (key l))
                     :value (val l)}))
       (into [])))

(defn ->service-log-options
  [service-task-template]
  (let [log-driver (get-in service-task-template [:LogDriver :Options])]
    (->> log-driver
         (map (fn [l] {:name  (name (key l))
                       :value (val l)}))
         (into []))))

(defn ->service-placement-constraints
  [service-spec]
  (->> (get-in service-spec [:TaskTemplate :Placement :Constraints])
       (map (fn [v] {:rule v}))
       (into [])))

(defn ->service-resource
  [service-resource-category]
  (let [nano-cpu (:NanoCPUs service-resource-category)
        memory-bytes (:MemoryBytes service-resource-category)]
    {:cpu    (when (some? nano-cpu)
               (-> (/ nano-cpu 1000000000)
                   (double)))
     :memory (when (some? memory-bytes)
               (as-megabytes memory-bytes))}))

(defn ->service-secrets
  [service-spec]
  (->> (get-in service-spec [:TaskTemplate :ContainerSpec :Secrets])
       (map (fn [s] {:id         (:SecretID s)
                     :secretName (:SecretName s)
                     :uid        (get-in s [:File :UID])
                     :gid        (get-in s [:File :GID])
                     :mode       (get-in s [:File :Mode])}))
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
  (let [value (:swarmpit.service.deployment.autoredeploy service-labels)]
    (if (some? value)
      (= "true" value)
      nil)))

(defn ->service-image-details
  [image-name]
  (let [separator-pos (str/last-index-of image-name ":")
        length (count image-name)]
    {:name (subs image-name 0 separator-pos)
     :tag  (subs image-name (inc separator-pos) length)}))

(defn ->service
  [service tasks]
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
      :distribution {:id   (:swarmpit.service.distribution.id service-labels)
                     :type (:swarmpit.service.distribution.type service-labels)}
      :repository (merge (->service-image-details image-name)
                         {:image       image-name
                          :imageId     (:swarmpit.service.repository.image.id service-labels)
                          :imageDigest image-digest})
      :serviceName service-name
      :mode service-mode
      :replicas replicas
      :state (if (= service-mode "replicated")
               (->service-state replicas-running replicas)
               (->service-state replicas-running replicas-no-shutdown))
      :status {:info    (if (= service-mode "replicated")
                          (->service-info-status replicas-running replicas)
                          (->service-info-status replicas-running replicas-no-shutdown))
               :update  (get-in service [:UpdateStatus :State])
               :message (get-in service [:UpdateStatus :Message])}
      :ports (->service-ports service-spec)
      :mounts (->service-mounts service-spec)
      :secrets (->service-secrets service-spec)
      :variables (->service-variables service-spec)
      :labels (->service-labels service-labels)
      :logdriver {:name (or (get-in service-task-template [:LogDriver :Name]) "none")
                  :opts (->service-log-options service-task-template)}
      :resources {:reservation (->service-resource (get-in service-task-template [:Resources :Reservations]))
                  :limit       (->service-resource (get-in service-task-template [:Resources :Limits]))}
      :deployment {:update          (->service-deployment-update service-spec)
                   :forceUpdate     (:ForceUpdate service-task-template)
                   :restartPolicy   (->service-deployment-restart-policy service-task-template)
                   :rollback        (->service-deployment-rollback service-spec)
                   :rollbackAllowed (some? (:PreviousSpec service))
                   :autoredeploy    (->service-autoredeploy service-labels)
                   :placement       (->service-placement-constraints service-spec)})))

(defn ->services
  [services tasks]
  (->> services
       (map #(->service % tasks))
       (into [])))

(defn ->volume
  [volume]
  (let [name (:Name volume)]
    (array-map
      :id name
      :volumeName name
      :driver (:Driver volume)
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