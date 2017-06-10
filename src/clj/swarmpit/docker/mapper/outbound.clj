(ns swarmpit.docker.mapper.outbound
  "Map swarmpit domain to docker domain"
  (:require [clojure.string :as str]))

(defn ->auth-config
  [registry]
  {:username      (:username registry)
   :password      (:password registry)
   :serveraddress (:url registry)})

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

(defn ->service-image-create
  [service registry]
  (let [image (get-in service [:repository :imageName])
        tag (get-in service [:repository :imageTag])]
    (if (= "dockerhub" (:name registry))
      (str image ":" tag)
      (str (:url registry) "/" image ":" tag))))

(defn ->service-image-update
  [service]
  (let [image (get-in service [:repository :imageName])
        tag (get-in service [:repository :imageTag])]
    (str image ":" tag)))

(defn ->service
  [service image]
  {:Name (:serviceName service)
   :TaskTemplate
         {:ContainerSpec
          {:Image  image
           :Mounts (->service-volumes service)
           :Env    (->service-variables service)}}
   :Mode (->service-mode service)
   :EndpointSpec
         {:Ports (->service-ports service)}})

(defn ->network
  [network]
  {:Name     (:networkName network)
   :Driver   (:driver network)
   :Internal (:internal network)})