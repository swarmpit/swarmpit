(ns swarmpit.domain
  (:require [clojure.string :as str]))

(defn ->service-mode
  [service]
  (if (= (:mode service) "global")
    {:Global {}}
    {:Replicated
     {:Replicas (:replicas service)}}))

(defn ->service-ports
  [service]
  (->> (:ports service)
       (map (fn [p] {:Protocol      (:protocol p)
                     :PublishedPort (Integer. (:hostPort p))
                     :TargetPort    (Integer. (:containerPort p))}))
       (into [])))

(defn ->service-variables
  [service]
  (->> (:variables service)
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
