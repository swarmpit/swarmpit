(ns swarmpit.docker.engine.mapper.compose
  (:require [clojure.string :as str]
            [clojure.set :refer [rename-keys]]
            [swarmpit.utils :refer [clean select-keys*]]
            [swarmpit.yaml :refer [->yaml]])
  (:refer-clojure :exclude [alias]))

(defn alias
  [key stackName map]
  (let [name (get map key)]
    (if (= stackName (:stack map))
      (str/replace name (str stackName "_") "")
      name)))

(defn group
  [stackName fn coll]
  (->> coll
       (map #(fn stackName %))
       (into {})))

(defn environment
  [variables]
  (map #(str (:name %) "=" (:value %)) variables))

(defn service-volumes
  [mounts]
  (map #(str (:host %) "=" (:containerPath %) (when (:readOnly %) ":ro")) mounts))

(defn resource
  [{:keys [cpu memory]}]
  {:cpus   (some-> cpu (str))
   :memory (some-> memory (str "M"))})

(defn service
  [stackName service]
  {(keyword (alias :serviceName stackName service))
   {:image       (-> service :repository :image)
    :environment (->> service :variables
                      (map #(str (:name %) "=" (:value %))))
    :ports       (->> service :ports
                      (map #(str (:hostPort %) ":" (:containerPort %) (when (= "udp" (:protocol %)) "/udp"))))
    :volumes     (->> service :mounts
                      (map #(str (alias :host stackName %) "=" (:containerPath %) (when (:readOnly %) ":ro"))))
    :networks    (->> service :networks (map #(alias :networkName stackName %)))
    :deploy      {:mode      (when-not (= "replicated" (:mode service)) (:mode service))
                  :placement {:constraints (->> service :deployment :placement (map :rule))}
                  :resources {:reservations (-> service :resources :reservation resource)
                              :limits       (-> service :resources :limit resource)}}}})

(defn network
  [stackName net]
  {(keyword (alias :networkName stackName net)) (select-keys net [:driver])})

(defn volume
  [stackName volume]
  {(keyword (alias :volumeName stackName volume)) (select-keys volume [:driver])})

(defn ->compose
  [stack]
  (let [name (:stackName stack)]
    (-> {:version  "3"
         :services (group name service (:services stack))
         :networks (group name network (:networks stack))
         :volumes  (group name volume (:volumes stack))}
        (clean))))