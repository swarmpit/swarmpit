(ns swarmpit.stats
  (:require [clojure.core.cache :as cache]
            [clojure.core.memoize :as memo]
            [clojure.tools.logging :as log]
            [swarmpit.docker.engine.client :as docker]
            [swarmpit.influxdb.client :as influx]
            [swarmpit.influxdb.mapper :as m]
            [swarmpit.utils :refer [nano-> as-MiB]]
            [swarmpit.config :refer [config]]))

(def nodes-memo (memo/ttl docker/nodes :ttl/threshold 5000))

(defn active-hosts
  "List of running nodes"
  []
  (->> (nodes-memo)
       (filter #(= "ready" (get-in % [:Status :State])))
       (map :ID)
       (set)))

(defn host-cpus
  "Number of host CPUs"
  [host-id]
  (-> (->> (nodes-memo)
           (filter #(= host-id (:ID %)))
           (first))
      (get-in [:Description :Resources :NanoCPUs])
      (nano->)))

(defn cluster-cpus
  "Number of cluster CPUs"
  []
  (->> (nodes-memo)
       (filter #(= "ready" (get-in % [:Status :State])))
       (map #(get-in % [:Description :Resources :NanoCPUs]))
       (apply +)
       (nano->)))

(def cache (atom (cache/basic-cache-factory {})))

(defn influx-configured? []
  (some? (config :influxdb-url)))

(defn ready? []
  (some? (and (not-empty @cache)
              (not-empty (active-hosts)))))

(defn store-to-cache
  "Store stats in local cache"
  [stats]
  (swap! cache assoc (:id stats) stats))

(defn store-to-db
  "Store stats in influxDB as timeseries"
  [{:keys [id tasks] :as stats}]
  (when (influx-configured?)
    (let [host-tags (m/->host-tags id)]
      (influx/write-host-points host-tags stats)
      (doseq [{:keys [name] :as task-stats} tasks]
        (when-let [task-tags (m/->task-tags name id)]
          (influx/write-task-points task-tags task-stats))))))

(defn node
  "Get latest node stats from local cache"
  [node-id]
  (let [stats (get @cache node-id)]
    (when stats
      (assoc-in stats [:cpu :cores] (host-cpus node-id)))))

(defn task-raw
  "Get latest raw task stats from local cache"
  [task]
  (->> (node (:nodeId task))
       :tasks
       (filter #(= (str "/" (:taskName task) "." (:id task))
                   (:name %)))
       (first)))

(defn task
  "Get latest standardized task stats from local cache"
  [task]
  (let [host-cpus (host-cpus (:nodeId task))
        task-stats (task-raw task)]
    (when task-stats
      (let [resource-cpu-limit (get-in task [:resources :limit :cpu])
            cpu (/ (:cpuPercentage task-stats) 100)
            cpu-limit (if (zero? resource-cpu-limit)
                        host-cpus
                        resource-cpu-limit)
            cpu-percentage (/ (:cpuPercentage task-stats) cpu-limit)]
        (-> task-stats
            (assoc :cpu (if (> cpu cpu-limit) cpu-limit cpu))
            (assoc :cpuPercentage (if (> cpu-percentage 100) 100 cpu-percentage))
            (assoc :cpuLimit cpu-limit))))))

(defn hosts-resources
  []
  "Get hosts resources"
  (->> (nodes-memo)
       (filter #(= "ready" (get-in % [:Status :State])))
       (map #(let [nano-cpu (get-in % [:Description :Resources :NanoCPUs])
                   memory-bytes (get-in % [:Description :Resources :MemoryBytes])]
               (hash-map (:ID %) {:cores  (nano-> nano-cpu)
                                  :memory memory-bytes})))
       (into {})))

(defn cluster
  "Get latest cluster statistics"
  []
  (let [cached-hosts (vals @cache)
        active-hosts (active-hosts)
        hosts (filter #(contains? active-hosts (:id %)) cached-hosts)
        sum-fn (fn [ks] (reduce + (map #(get-in % ks) hosts)))
        mean-fn (fn [ks] (/ (sum-fn ks) (count hosts)))]
    {:resources (hosts-resources)
     :cpu       {:usage (mean-fn [:cpu :usedPercentage])
                 :cores (cluster-cpus)}
     :memory    {:usage (mean-fn [:memory :usedPercentage])
                 :used  (sum-fn [:memory :used])
                 :total (sum-fn [:memory :total])}
     :disk      {:usage (mean-fn [:disk :usedPercentage])
                 :used  (sum-fn [:disk :used])
                 :total (sum-fn [:disk :total])}}))

(defn task-timeseries
  "Get task timeseries data for last 24 hours"
  [task-name]
  (-> (influx/read-task-stats task-name)
      (first)
      (get "series")
      (first)
      (m/->task-ts)))

(def task-timeseries-memo (memo/ttl task-timeseries :ttl/threshold 5000))

(defn ->services
  "Get service name col from top series result"
  [top-series]
  (map #(last %) (-> top-series
                     (first)
                     (get "series")
                     (first)
                     (get "values"))))

(defn services-top-cpu-timeseries
  "Get top 10 services CPU usage for last 24 hours"
  []
  (let [top-10-series (influx/read-services-top-cpu-usage)]
    (->services top-10-series)))

(def services-top-cpu-memo (memo/ttl services-top-cpu-timeseries :ttl/threshold 60000))
(def services-cpu-stats-memo (memo/ttl influx/read-services-cpu-stats :ttl/threshold 5000))

(defn services-top-ram-timeseries
  "Get top 10 services Memory usage for last 24 hours"
  []
  (let [top-10-series (influx/read-services-top-memory-usage)]
    (->services top-10-series)))

(def services-top-ram-memo (memo/ttl services-top-ram-timeseries :ttl/threshold 60000))
(def services-ram-stats-memo (memo/ttl influx/read-services-memory-stats :ttl/threshold 5000))

(defn services-timeseries
  "Get services timeseries data for last 24 hours"
  []
  {:cpu    (map #(m/->service-cpu-ts %)
                (-> (services-top-cpu-memo)
                    (services-cpu-stats-memo)
                    (first)
                    (get "series")))
   :memory (map #(m/->service-memory-ts %)
                (-> (services-top-ram-memo)
                    (services-ram-stats-memo)
                    (first)
                    (get "series")))})

(def services-timeseries-memo (memo/ttl services-timeseries :ttl/threshold 5000))

(defn hosts-timeseries
  "Get hosts timeseries data for last 24 hours"
  []
  (let [nodes (->> (nodes-memo)
                   (group-by :ID))
        node-name #(or (-> (get nodes %) first :Description :Hostname) %)]
    (->> (-> (influx/read-host-stats)
             (first)
             (get "series"))
         (map #(m/->host-ts %))
         (map #(update % :name node-name)))))

(def hosts-timeseries-memo (memo/ttl hosts-timeseries :ttl/threshold 5000))