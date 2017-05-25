(ns swarmpit.docker.mapper.inbound
  "Map docker domain to swarmpit domain"
  (:require [clojure.string :as str]))

(defn ->node
  [node]
  (array-map
    :id (get node :ID)
    :nodeName (get-in node [:Description :Hostname])
    :role (get-in node [:Spec :Role])
    :availability (get-in node [:Spec :Availability])
    :state (get-in node [:Status :State])
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
                    (str service-name "." slot "." id)
                    (str service-name "." id))]
    (array-map
      :id id
      :taskName task-name
      :version (get-in task [:Version :Index])
      :createdAt (get task :CreatedAt)
      :updatedAt (get task :UpdatedAt)
      :image image-name
      :imageDigest image-digest
      :state (get-in task [:Status :State])
      :desiredState (get task :DesiredState)
      :serviceName service-name
      :node (->task-node task nodes))))

(defn ->tasks
  [tasks nodes service-name service-mode]
  (->> tasks
       (map #(->task % nodes service-name service-mode))
       (into [])))

(defn ->service-ports
  [service]
  (->> (get-in service [:Spec :EndpointSpec :Ports])
       (map (fn [p] {:containerPort (:TargetPort p)
                     :protocol      (:Protocol p)
                     :hostPort      (:PublishedPort p)}))
       (into [])))

(defn ->service-volumes
  [service]
  (->> (get-in service [:Spec :TaskTemplate :ContainerSpec :Mounts])
       (map (fn [v] {:containerPath (:Source v)
                     :hostPath      (:Target v)
                     :readOnly      (:ReadOnly v)}))
       (into [])))

(defn ->service-variables
  [service]
  (->> (get-in service [:Spec :TaskTemplate :ContainerSpec :Env])
       (map (fn [p]
              (let [variable (str/split p #"=")]
                {:name  (first variable)
                 :value (second variable)})))
       (into [])))

(defn ->service-replicas-state
  [replicas replicas-running service-mode]
  (if (= service-mode "replicated")
    (str replicas-running " / " replicas)
    (str replicas-running " / " replicas-running)))

(defn ->service-state
  [replicas replicas-running service-mode]
  (case service-mode
    "replicated" (if (zero? replicas-running)
                   "not running"
                   (if (= replicas-running replicas)
                     "running"
                     "partly running"))
    "global" (if (zero? replicas-running)
               "not running"
               "running")))

(defn ->service
  [service tasks nodes]
  (let [service-mode (str/lower-case (name (first (keys (get-in service [:Spec :Mode])))))
        service-name (get-in service [:Spec :Name])
        service-id (get service :ID)
        service-tasks (-> (filter #(= (:ServiceID %) service-id) tasks)
                          (->tasks nodes service-name service-mode))
        replicas (get-in service [:Spec :Mode :Replicated :Replicas])
        replicas-running (-> (filter #(= (:state %) "running") service-tasks)
                             (count))
        image (get-in service [:Spec :TaskTemplate :ContainerSpec :Image])
        image-info (str/split image #"@")
        image-name (first image-info)
        image-digest (second image-info)]
    (array-map
      :id service-id
      :version (get-in service [:Version :Index])
      :createdAt (get service :CreatedAt)
      :updatedAt (get service :UpdatedAt)
      :image image-name
      :imageDigest image-digest
      :serviceName service-name
      :mode service-mode
      :replicas replicas
      :replicasState (->service-replicas-state replicas replicas-running service-mode)
      :state (->service-state replicas replicas-running service-mode)
      :ports (->service-ports service)
      :volumes (->service-volumes service)
      :variables (->service-variables service)
      :tasks service-tasks)))

(defn ->services
  [services tasks nodes]
  (->> services
       (map #(->service % tasks nodes))
       (into [])))

(defn ->network
  [network]
  (let [config (first (get-in network [:IPAM :Config]))]
    (array-map
      :id (get network :Id)
      :networkName (get network :Name)
      :created (get network :Created)
      :driver (get network :Driver)
      :internal (get network :Internal)
      :subnet (:Subnet config)
      :gateway (:Gateway config))))

(defn ->networks
  [networks]
  (->> networks
       (map ->network)
       (into [])))