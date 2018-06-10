(ns swarmpit.docker.engine.mapper.compose
  (:require [clojure.string :as str]
            [clojure.set :refer [rename-keys]]
            [swarmpit.utils :refer [clean select-keys* name-value->map]]
            [flatland.ordered.map :refer [ordered-map]]
            [swarmpit.docker.utils :refer [trim-stack]]
            [swarmpit.docker.engine.mapper.inbound :refer [autoredeploy-label]]
            [swarmpit.yaml :refer [->yaml]])
  (:refer-clojure :exclude [alias]))

(def compose-version "3.3")

(defn in-stack?
  [stack-name map]
  (when stack-name (= stack-name (:stack map))))

(defn alias
  [key stack-name map]
  (let [name (get map key)]
    (if (and name (in-stack? stack-name map))
      (trim-stack stack-name name)
      name)))

(defn group
  [stack-name fn coll]
  (->> coll
       (map #(fn stack-name %))
       (into (sorted-map))
       (ordered-map)))

(defn add-swarmpit-labels
  [service map]
  (merge map (when (-> service :deployment :autoredeploy) {autoredeploy-label "true"})))

(defn targetable
  [source-key target-key item]
  (->> item
       (map #(let [{name   source-key
                    target target-key} %]
               (if (= name target)
                 name
                 {:source name
                  :target target})))))

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
     :command (some->> service :command (str/join " "))
     :environment (-> service :variables (name-value->map))
     :ports (->> service :ports
                 (map #(str (:hostPort %) ":" (:containerPort %) (when (= "udp" (:protocol %)) "/udp"))))
     :volumes (->> service :mounts
                   (map #(str (alias :host stack-name %) ":" (:containerPath %) (when (:readOnly %) ":ro"))))
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
              :placement      {:constraints (->> service :deployment :placement (map :rule))}
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
    (-> {:version  compose-version
         :services (group name service (->> stack :services (sort-by :serviceName) (vec)))
         :networks (group name network (:networks stack))
         :volumes  (group name volume (:volumes stack))
         :configs  (group name config (:configs stack))
         :secrets  (group name secret (:secrets stack))}
        (clean))))