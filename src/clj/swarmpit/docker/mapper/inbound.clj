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

(defn ->service-replicas-running
  [service-tasks]
  (-> (filter #(= (get-in % [:Status :State]) "running") service-tasks)
      (count)))

(defn ->service-info-status
  [service-replicas service-replicas-running service-mode]
  (if (= service-mode "replicated")
    (str service-replicas-running " / " service-replicas)
    (str service-replicas-running " / " service-replicas-running)))

(defn ->service-update-status
  [service]
  (= "updating" (get-in service [:UpdateStatus :State])))

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

(defn ->service
  [service tasks nodes]
  (let [service-mode (str/lower-case (name (first (keys (get-in service [:Spec :Mode])))))
        service-name (get-in service [:Spec :Name])
        service-id (get service :ID)
        service-tasks (filter #(= (:ServiceID %) service-id) tasks)
        replicas (get-in service [:Spec :Mode :Replicated :Replicas])
        replicas-running (->service-replicas-running service-tasks)
        image (get-in service [:Spec :TaskTemplate :ContainerSpec :Image])
        image-info (str/split image #"@")
        image-name (first image-info)
        image-digest (second image-info)
        image-segments (str/split image-name #":")]
    (array-map
      :id service-id
      :version (get-in service [:Version :Index])
      :createdAt (date (get service :CreatedAt))
      :updatedAt (date (get service :UpdatedAt))
      :repository {:image       image-name
                   :imageDigest image-digest
                   :imageName   (first image-segments)
                   :imageTag    (second image-segments)}
      :serviceName service-name
      :mode service-mode
      :replicas replicas
      :state (->service-state replicas replicas-running service-mode)
      :status {:info   (->service-info-status replicas replicas-running service-mode)
               :update (->service-update-status service)}
      :ports (->service-ports service)
      :volumes (->service-volumes service)
      :variables (->service-variables service)
      :tasks (->tasks service-tasks nodes service-name service-mode))))

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
      :created (date (get network :Created))
      :driver (get network :Driver)
      :internal (get network :Internal)
      :subnet (:Subnet config)
      :gateway (:Gateway config))))

(defn ->networks
  [networks]
  (->> networks
       (map ->network)
       (filter #(or (= "bridge" (:driver %))
                    (= "overlay" (:driver %))))
       (into [])))