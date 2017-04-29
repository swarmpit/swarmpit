(ns swarmpit.domain
  (:require [clojure.string :as str]))

;;; Service domain

(defn ->service-mode
  [service]
  (if (= (:mode service) "global")
    {:Global {}}
    {:Replicated
     {:Replicas (:replicas service)}}))

(defn ->service-ports
  [service]
  (->> (:ports service)
       (filter #(and (> (:hostPort %) 0)
                     (> (:containerPort %) 0)))
       (map (fn [p] {:Protocol      (:protocol p)
                     :PublishedPort (:hostPort p)
                     :TargetPort    (:containerPort p)}))
       (into [])))

(defn ->service-variables
  [service]
  (->> (:variables service)
       (filter #(not (and (str/blank? (:name %))
                          (str/blank? (:value %)))))
       (map (fn [p] (str (:name p) "=" (:value p))))
       (into [])))

(defn ->service-volumes
  [service]
  (->> (:volumes service)
       (map (fn [v] {:ReadOnly (:readOnly v)
                     :Source   (:containerPath v)
                     :Target   (:hostPath v)
                     :Type     "volume"
                     :VolumeOptions
                               {:DriverConfig {}}}))
       (into [])))

(defn ->service
  "Map swarmpit service domain to docker service domain"
  [service]
  {:Name (:serviceName service)
   :TaskTemplate
         {:ContainerSpec
          {:Image  (:image service)
           :Mounts (->service-volumes service)
           :Env    (->service-variables service)}}
   :Mode (->service-mode service)
   :EndpointSpec
         {:Ports (->service-ports service)}})

(defn <-service-ports
  [service]
  (->> (get-in service [:Spec :EndpointSpec :Ports])
       (map (fn [p] {:containerPort (:TargetPort p)
                     :protocol      (:Protocol p)
                     :hostPort      (:PublishedPort p)}))
       (into [])))

(defn <-service-volumes
  [service]
  (->> (get-in service [:Spec :TaskTemplate :ContainerSpec :Mounts])
       (map (fn [v] {:containerPath (:Source v)
                     :hostPath      (:Target v)
                     :readOnly      (:ReadOnly v)}))
       (into [])))

(defn <-service-variables
  [service]
  (->> (get-in service [:Spec :TaskTemplate :ContainerSpec :Env])
       (map (fn [p]
              (let [variable (str/split p #"=")]
                {:name  (first variable)
                 :value (second variable)})))
       (into [])))

(defn <-service
  "Map docker service domain to swarmpit service domain"
  [service]
  (let [image (get-in service [:Spec :TaskTemplate :ContainerSpec :Image])
        image-info (str/split image #"@")
        image-name (first image-info)
        image-digest (second image-info)]
    (array-map
      :id (get service :ID)
      :version (get-in service [:Version :Index])
      :createdAt (get service :CreatedAt)
      :updatedAt (get service :UpdatedAt)
      :image image-name
      :imageDigest image-digest
      :serviceName (get-in service [:Spec :Name])
      :mode (str/lower-case (name (first (keys (get-in service [:Spec :Mode])))))
      :replicas (get-in service [:Spec :Mode :Replicated :Replicas])
      :ports (<-service-ports service)
      :volumes (<-service-volumes service)
      :variables (<-service-variables service))))

(defn <-services
  [services]
  (->> services
       (map <-service)
       (into [])))

;;; Network domain

(defn ->network
  "Map swarmpit network domain to docker network domain"
  [network]
  {:Name     (:name network)
   :Driver   (:driver network)
   :Internal (:internal network)})

(defn <-network-configs
  [network]
  (->> (get-in network [:IPAM :Config])
       (map (fn [n] {:subnet  (:Subnet n)
                     :range   (:IPRange n)
                     :gateway (:Gateway n)}))
       (into [])))

(defn <-network
  "Map docker network domain to swarmpit network domain"
  [network]
  (let [config (first (get-in network [:IPAM :Config]))]
    (array-map
      :id (get network :Id)
      :name (get network :Name)
      :created (get network :Created)
      :driver (get network :Driver)
      :internal (get network :Internal)
      :subnet (:Subnet config)
      :gateway (:Gateway config))))

(defn <-networks
  [networks]
  (->> networks
       (map <-network)
       (into [])))

;;; Node domain

(defn <-node
  "Map docker node domain to swarmpit node domain"
  [node]
  (array-map
    :id (get node :ID)
    :name (get-in node [:Description :Hostname])
    :role (get-in node [:Spec :Role])
    :availability (get-in node [:Spec :Availability])
    :state (get-in node [:Status :State])
    :leader (get-in node [:ManagerStatus :Leader])))

(defn <-nodes
  [nodes]
  (->> nodes
       (map <-node)
       (into [])))

;;; Task domain

(defn <-task
  "Map docker task domain to swarmpit task domain"
  [task services nodes]
  (let [image (get-in task [:Spec :ContainerSpec :Image])
        image-info (str/split image #"@")
        image-name (first image-info)
        image-digest (second image-info)
        service (get services (:ServiceID task))
        node (get nodes (:NodeID task))]
    (array-map
      :id (get task :ID)
      :name (str service "." (get task :Slot))
      :version (get-in task [:Version :Index])
      :createdAt (get task :CreatedAt)
      :updatedAt (get task :UpdatedAt)
      :image image-name
      :imageDigest image-digest
      :state (get-in task [:Status :State])
      :desiredState (get task :DesiredState)
      :service service
      :node node)))

(defn <-tasks
  [tasks services nodes]
  (->> tasks
       (map #(<-task % services nodes))
       (into [])))