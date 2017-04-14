(ns swarmpit.domain)

(defn ->service-mode
  [service]
  (if (= (:mode service) "global")
    "Global"
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

(defn ->service
  "Map swarmpit service domain to docker service domain"
  [service]
  {:Name (:serviceName service)
   :TaskTemplate
         {:ContainerSpec
          {:Image (:image service)
           :Env   (->service-variables service)}}
   :Mode (->service-mode service)
   :EndpointSpec
         {:Ports (->service-ports service)}})

(defn <-service
  "Map docker service domain to swarmpit service domain"
  [service])
