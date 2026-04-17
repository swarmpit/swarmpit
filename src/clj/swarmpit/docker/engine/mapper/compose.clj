(ns swarmpit.docker.engine.mapper.compose
  (:require [clojure.set :refer [rename-keys]]
            [flatland.ordered.map :refer [ordered-map]]
            [swarmpit.utils :refer [clean select-keys* name-value->map name-value->sorted-map]]
            [swarmpit.docker.utils :refer [trim-stack in-stack? alias]]
            [swarmpit.docker.engine.mapper.inbound :as mi]
            [swarmpit.yaml :refer [->yaml]])
  (:refer-clojure :exclude [alias]))


(defn group
  [stack-name fn coll]
  (->> coll
       (map #(fn stack-name %))
       (into (sorted-map))
       (ordered-map)))

(defn add-swarmpit-service-links
  [service]
  (->> (:links service)
       (map #(hash-map (keyword (str mi/link-label (:name %))) (:value %)))
       (into {})))

(defn add-swarmpit-labels
  [service map]
  (merge map
         (when (-> service :agent)
           {mi/agent-label "true"})
         (when (-> service :deployment :autoredeploy)
           {mi/autoredeploy-label "true"})
         (when (-> service :immutable)
           {mi/immutable-label "true"})
         (when (not-empty (:links service))
           (add-swarmpit-service-links service))))

(defn targetable
  [source-key target-key item]
  (->> item
       (map (fn [m]
              (let [source (get m source-key)
                    target (get m target-key)
                    uid    (:uid m)
                    gid    (:gid m)
                    mode   (:mode m)
                    long?  (or (some? uid) (some? gid) (some? mode)
                               (and target (not= source target)))]
                (if long?
                  (cond-> (ordered-map :source source)
                    (and target (not= source target)) (assoc :target target)
                    (some? uid)                       (assoc :uid uid)
                    (some? gid)                       (assoc :gid gid)
                    (some? mode)                      (assoc :mode mode))
                  source))))))

(defn resource
  [{:keys [cpu memory]}]
  {:cpus   (when (< 0 cpu) (str cpu))
   :memory (when (< 0 memory) (str memory "M"))})

(defn remove-defaults
  [map defaults]
  (->> (keys defaults)
       (filter #(= (% defaults) (% map)))
       (apply dissoc map)))

(defn service
  [stack-name service]
  {(keyword (alias :serviceName (or stack-name (:stack service)) service))
   (ordered-map
     :image (-> service :repository :image)
     :entrypoint (some->> service :entrypoint)
     :command (some->> service :command)
     :hostname (-> service :hostname)
     :isolation (-> service :isolation)
     :sysctls (->> service :sysctls (name-value->map))
     :labels (->> service :containerLabels (name-value->map))
     :user (-> service :user)
     :working_dir (-> service :dir)
     :extra_hosts (->> service :hosts
                       (map #(str (:name %) ":" (:value %))))
     :healthcheck (let [healthcheck (-> service :healthcheck)]
                    (when healthcheck
                      (merge healthcheck
                             {:interval (str (:interval healthcheck) "s")}
                             {:timeout (str (:timeout healthcheck) "s")})))
     :tty (-> service :tty)
     :environment (-> service :variables (name-value->sorted-map))
     :ports (->> service :ports
                 (map (fn [p]
                        (let [protocol (:protocol p)
                              mode (:mode p)
                              non-tcp? (and protocol (not= "tcp" protocol))
                              non-ingress? (and mode (not= "ingress" mode))]
                          (if (or non-tcp? non-ingress?)
                            (cond-> (ordered-map :target (:containerPort p)
                                                 :published (:hostPort p))
                              non-tcp?     (assoc :protocol protocol)
                              non-ingress? (assoc :mode mode))
                            (str (:hostPort p) ":" (:containerPort p)))))))
     :volumes (->> service :mounts
                   (map (fn [m]
                          (if (= "tmpfs" (:type m))
                            (cond-> (ordered-map :type "tmpfs"
                                                 :target (:containerPath m))
                              (:readOnly m) (assoc :read_only true))
                            (str (alias :host stack-name m) ":" (:containerPath m) (when (:readOnly m) ":ro"))))))
     :networks (->> service :networks (map #(alias :networkName stack-name %)))
     :secrets (->> service :secrets (targetable :secretName :secretTarget))
     :configs (->> service :configs (targetable :configName :configTarget))
     :logging {:driver  (-> service :logdriver :name)
               :options (-> service :logdriver :opts (name-value->map))}
     :deploy {:mode           (when-not (= "replicated" (:mode service)) (:mode service))
              :replicas       (some-> (:replicas service) (#(when (not (= 1 %)) %)))
              :labels         (->> service :labels (name-value->map) (add-swarmpit-labels service))
              :update_config  (-> service :deployment :update
                                  (rename-keys {:failureAction :failure_action})
                                  (update :delay #(str % "s"))
                                  (remove-defaults
                                    {:parallelism    1
                                     :delay          "0s"
                                     :order          "stop-first"
                                     :failure_action "pause"}))
              :restart_policy (-> service :deployment :restartPolicy
                                  (rename-keys {:attempts :max_attempts})
                                  (update :delay #(str % "s"))
                                  (update :window #(str % "s"))
                                  (remove-defaults
                                    {:condition    "any"
                                     :delay        "5s"
                                     :window       "0s"
                                     :max_attempts 0}))
              :placement      {:constraints          (->> service :deployment :placement (map :rule))
                               :max_replicas_per_node (get-in service [:deployment :maxReplicas])}
              :resources      {:reservations (-> service :resources :reservation resource)
                               :limits       (-> service :resources :limit resource)}})})

(defn network
  [stack-name net]
  {(keyword (alias :networkName stack-name net))
   (if (in-stack? stack-name net)
     {:driver      (:driver net)
      :internal    (when (:internal net) true)
      :driver_opts (-> (:options net)
                       (name-value->map)
                       (dissoc :com.docker.network.driver.overlay.vxlanid_list))}
     {:external true})})

(defn volume
  [stack-name volume]
  {(keyword (alias :volumeName stack-name volume))
   (if (in-stack? stack-name volume)
     {:driver      (:driver volume)
      :driver_opts (-> volume :options (name-value->map))}
     {:external true})})

(defn secret
  [_ secret]
  {(keyword (:secretName secret))
   {:external true}})

(defn config
  [_ config]
  {(keyword (:configName config))
   {:external true}})

(defn ->compose
  [stack]
  (let [name (:stackName stack)]
    (-> {:services (group name service (->> stack :services (sort-by :serviceName) (vec)))
         :networks (group name network (:networks stack))
         :volumes  (group name volume (:volumes stack))
         :configs  (group name config (:configs stack))
         :secrets  (group name secret (:secrets stack))}
        (clean))))
