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

(defn- ->service-networks
  [service]
  (->> (:networks service)
       (map (fn [n] {:Target (:networkName n)}))
       (into [])))

(defn ->service-variables
  [service]
  (->> (:variables service)
       (filter #(not (and (str/blank? (:name %))
                          (str/blank? (:value %)))))
       (map (fn [p] (str (:name p) "=" (:value p))))
       (into [])))

(defn ->service-mounts
  [service]
  (->> (:mounts service)
       (map (fn [v] {:ReadOnly (:readOnly v)
                     :Source   (:containerPath v)
                     :Target   (:hostPath v)
                     :Type     "volume"
                     :VolumeOptions
                               {:DriverConfig {}}}))
       (into [])))

(defn ->service-update-config
  [service]
  (let [deployment (:deployment service)]
    {:Parallelism    (:parallelism deployment)
     :Delay          (:delay deployment)
     ::FailureAction (:failureAction deployment)}))

(defn ->service-image-registry
  [service registry]
  (let [image (get-in service [:repository :imageName])
        tag (get-in service [:repository :imageTag])]
    (str (:url registry) "/" image ":" tag)))

(defn ->service-image
  [service]
  (let [image (get-in service [:repository :imageName])
        tag (get-in service [:repository :imageTag])]
    (str image ":" tag)))

(defn ->service
  [service image]
  {:Name         (:serviceName service)
   :TaskTemplate {:ContainerSpec
                            {:Image  image
                             :Mounts (->service-mounts service)
                             :Env    (->service-variables service)}
                  :Networks (->service-networks service)}
   :Mode         (->service-mode service)
   :UpdateConfig (->service-update-config service)
   :EndpointSpec {:Ports (->service-ports service)}})

(defn ->network-ipam
  [network]
  (let [ipam (:ipam network)
        gateway (:gateway ipam)
        subnet (:subnet ipam)]
    (if (and (not (str/blank? gateway))
             (not (str/blank? subnet)))
      {:Config [{:Subnet  subnet
                 :Gateway gateway}]})))

(defn ->network
  [network]
  {:Name     (:networkName network)
   :Driver   (:driver network)
   :Internal (:internal network)
   :IPAM     (->network-ipam network)})