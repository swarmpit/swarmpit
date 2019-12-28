(ns swarmpit.docker.engine.mapper.outbound
  "Map swarmpit domain to docker domain"
  (:require [clojure.string :as str]
            [swarmpit.docker.engine.mapper.inbound :as mi]
            [swarmpit.utils :refer [name-value->map ->nano as-bytes]]))

(defn ->auth-config
  "Pass registry or dockeruser entity"
  [auth-entity]
  (when (some? auth-entity)
    {:username      (:username auth-entity)
     :password      (:password auth-entity)
     :serveraddress (:url auth-entity)}))

(defn ->service-mode
  [service]
  (if (= (:mode service) "global")
    {:Global {}}
    {:Replicated
     {:Replicas (:replicas service)}}))

(defn ->service-ports
  [service]
  (->> (:ports service)
       (filter #(and (some? (:containerPort %))
                     (some? (:hostPort %))
                     (> (:containerPort %) 0)
                     (> (:hostPort %) 0)))
       (map (fn [p] {:Protocol      (:protocol p)
                     :PublishMode   (:mode p)
                     :PublishedPort (:hostPort p)
                     :TargetPort    (:containerPort p)}))
       (into [])))

(defn- ->service-networks
  [service]
  (->> (:networks service)
       (map #(hash-map :Target (:networkName %)
                       :Aliases (:serviceAliases %)))
       (into [])))

(defn ->service-hosts
  [service]
  (->> (:hosts service)
       (filter #(not (and (str/blank? (:name %))
                          (str/blank? (:value %)))))
       (map (fn [p] (str (:value p) " " (:name p))))
       (into [])))

(defn ->service-variables
  [service]
  (->> (:variables service)
       (filter #(not (and (str/blank? (:name %))
                          (str/blank? (:value %)))))
       (map (fn [p] (str (:name p) "=" (:value p))))
       (into [])))

(defn ->service-labels
  [service]
  (->> (:labels service)
       (filter #(not (and (str/blank? (:name %))
                          (str/blank? (:value %)))))
       (name-value->map)))

(defn ->service-container-labels
  [service]
  (->> (:containerLabels service)
       (name-value->map)))

(defn ->service-log-options
  [service]
  (->> (get-in service [:logdriver :opts])
       (filter #(not (and (str/blank? (:name %))
                          (str/blank? (:value %)))))
       (name-value->map)))

(defn ->service-volume-options
  [service-volume]
  (when (some? service-volume)
    {:Labels       (:labels service-volume)
     :DriverConfig {:Name    (get-in service-volume [:driver :name])
                    :Options (name-value->map (get-in service-volume [:driver :options]))}}))

(defn ->service-mounts
  [service]
  (->> (:mounts service)
       (map (fn [v] {:ReadOnly      (:readOnly v)
                     :Source        (:host v)
                     :Target        (:containerPath v)
                     :Type          (:type v)
                     :VolumeOptions (->service-volume-options (:volumeOptions v))}))
       (into [])))

(defn- ->service-placement-contraints
  [service]
  (->> (get-in service [:deployment :placement])
       (map (fn [p] (:rule p)))
       (into [])))

(defn- ->secret-id
  [secret-name secrets]
  (->> secrets
       (filter #(= secret-name (:secretName %)))
       (first)
       :id))

(defn ->secret-target
  [secret]
  (let [secret-target (:secretTarget secret)]
    (if (str/blank? secret-target)
      (:secretName secret)
      secret-target)))

(defn ->service-secrets
  [service secrets]
  (->> (:secrets service)
       (map (fn [s] {:SecretName (:secretName s)
                     :SecretID   (->secret-id (:secretName s) secrets)
                     :File       {:GID  "0"
                                  :Mode 292
                                  :Name (->secret-target s)
                                  :UID  "0"}}))
       (into [])))

(defn- ->config-id
  [config-name configs]
  (->> configs
       (filter #(= config-name (:configName %)))
       (first)
       :id))

(defn ->config-target
  [config]
  (let [config-target (:configTarget config)]
    (if (str/blank? config-target)
      (:configName config)
      config-target)))

(defn ->service-configs
  [service configs]
  (->> (:configs service)
       (map (fn [c] {:ConfigName (:configName c)
                     :ConfigID   (->config-id (:configName c) configs)
                     :File       {:GID  "0"
                                  :Mode 292
                                  :Name (->config-target c)
                                  :UID  "0"}}))
       (into [])))

(defn ->service-resource
  [service-resource]
  (let [cpu (:cpu service-resource)
        memory (:memory service-resource)]
    {:NanoCPUs    (when (some? cpu)
                    (-> cpu
                        (->nano)
                        (long)))
     :MemoryBytes (when (some? memory)
                    (as-bytes memory))}))

(defn ->service-update-config
  [service]
  (let [update (get-in service [:deployment :update])]
    {:Parallelism   (:parallelism update)
     :Delay         (->nano (:delay update))
     :Order         (:order update)
     :FailureAction (:failureAction update)}))

(defn ->service-rollback-config
  [service]
  (let [rollback (get-in service [:deployment :rollback])]
    {:Parallelism   (:parallelism rollback)
     :Delay         (->nano (:delay rollback))
     :Order         (:order rollback)
     :FailureAction (:failureAction rollback)}))

(defn ->service-restart-policy
  [service]
  (let [policy (get-in service [:deployment :restartPolicy])]
    {:Condition   (:condition policy)
     :Delay       (->nano (:delay policy))
     :Window      (->nano (:window policy))
     :MaxAttempts (:attempts policy)}))

(defn ->service-healthcheck
  [service-healthcheck]
  (when service-healthcheck
    {:Test     (:test service-healthcheck)
     :Interval (->nano (:interval service-healthcheck))
     :Timeout  (->nano (:timeout service-healthcheck))
     :Retries  (:retries service-healthcheck)}))

(defn ->service-image
  [service digest?]
  (let [repository (get-in service [:repository :name])
        tag (get-in service [:repository :tag])
        digest (get-in service [:repository :imageDigest])]
    (if digest?
      (str repository ":" tag "@" digest)
      (str repository ":" tag))))

(defn ->service-links
  [service]
  (->> (:links service)
       (map #(hash-map (keyword (str mi/link-label (:name %))) (:value %)))
       (into {})))

(defn ->service-metadata
  [service image]
  (let [autoredeploy (get-in service [:deployment :autoredeploy])
        agent (:agent service)
        stack (:stack service)
        immutable (:immutable service)
        links (:links service)]
    (merge {}
           (when (some? stack)
             {:com.docker.stack.namespace stack
              :com.docker.stack.image     image})
           (when (some? autoredeploy)
             {mi/autoredeploy-label (str autoredeploy)})
           (when (some? agent)
             {mi/agent-label (str agent)})
           (when (some? immutable)
             {mi/immutable-label (str immutable)})
           (when (not-empty links)
             (->service-links service)))))

(defn ->service-container-metadata
  [service]
  (let [stack (:stack service)]
    (merge {}
           (when (some? stack)
             {:com.docker.stack.namespace stack}))))

(defn ->service
  [service]
  {:Name           (:serviceName service)
   :Labels         (merge
                     (->service-labels service)
                     (->service-metadata service (->service-image service false)))
   :TaskTemplate   {:ContainerSpec {:Image       (->service-image service true)
                                    :Labels      (merge
                                                   (->service-container-labels service)
                                                   (->service-container-metadata service))
                                    :Mounts      (->service-mounts service)
                                    :Secrets     (:secrets service)
                                    :Configs     (:configs service)
                                    :Args        (:command service)
                                    :TTY         (:tty service)
                                    :Healthcheck (->service-healthcheck (:healthcheck service))
                                    :Env         (->service-variables service)
                                    :Hosts       (->service-hosts service)}
                    :LogDriver     {:Name    (get-in service [:logdriver :name])
                                    :Options (->service-log-options service)}
                    :Resources     {:Limits       (->service-resource (get-in service [:resources :limit]))
                                    :Reservations (->service-resource (get-in service [:resources :reservation]))}
                    :RestartPolicy (->service-restart-policy service)
                    :Placement     {:Constraints (->service-placement-contraints service)}
                    :ForceUpdate   (get-in service [:deployment :forceUpdate])
                    :Networks      (->service-networks service)}
   :Mode           (->service-mode service)
   :UpdateConfig   (->service-update-config service)
   :RollbackConfig (->service-rollback-config service)
   :EndpointSpec   {:Ports (->service-ports service)}})

(defn ->network-ipam-config
  [network]
  (let [ipam (:ipam network)
        gateway (:gateway ipam)
        subnet (:subnet ipam)]
    (if (not (str/blank? subnet))
      {:Config [{:Subnet  subnet
                 :Gateway gateway}]}
      {:Config []})))

(defn ->network
  [network]
  {:Name       (:networkName network)
   :Driver     (:driver network)
   :Internal   (:internal network)
   :Options    (name-value->map (:options network))
   :Attachable (:attachable network)
   :Ingress    (:ingress network)
   :EnableIPv6 (:enableIPv6 network)
   :IPAM       (merge {:Driver "default"}
                      (->network-ipam-config network))})

(defn ->volume
  [volume]
  {:Name       (:volumeName volume)
   :Driver     (:driver volume)
   :DriverOpts (name-value->map (:options volume))
   :Labels     (:labels volume)})

(defn ->secret
  [secret]
  {:Name (:secretName secret)
   :Data (:data secret)})

(defn ->config
  [config]
  {:Name (:configName config)
   :Data (:data config)})

(defn ->node
  [node]
  {:Availability (:availability node)
   :Role         (:role node)
   :Labels       (name-value->map (:labels node))})
