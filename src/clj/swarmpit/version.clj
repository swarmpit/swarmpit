(ns swarmpit.version
  (:require [swarmpit.config :as cfg]
            [clojure.java.io :as io]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.core.memoize :as memo]
            [swarmpit.api :as api]
            [swarmpit.docker.engine.client :as client]
            [swarmpit.utils :refer [freq-of filter-by]]
            [swarmpit.couchdb.client :as db]
            [buddy.core.hash :as hash]
            [buddy.core.codecs :refer [bytes->hex]])
  (:import (java.util Properties)))

(def pom-properties
  (doto (Properties.)
    (.load (-> "META-INF/maven/swarmpit/swarmpit/pom.properties"
               (io/resource)
               (io/reader)))))

(def initialized?
  (memo/ttl api/admin-exists? :ttl/threshold 1000))

(defn short-info
  "Used for routes in compile time"
  []
  {:name     "swarmpit"
   :version  (get pom-properties "version")
   :revision (get pom-properties "revision")})

(defn info
  []
  (merge (short-info)
         {:initialized (initialized?)
          :statistics  (some? (cfg/config :influxdb-url))
          :docker      {:api    (read-string (cfg/config :docker-api))
                        :engine (cfg/config :docker-engine)}}))

(defn long-info
  []
  (let [stats [:mounts :networks :secrets :configs :hosts :variables :labels :ports :containerLabels]
        registry-stats [:v2 :dockerhub :ecr :acr :gitlab]
        registries (zipmap registry-stats (->> registry-stats (map #(count (db/find-docs (str (symbol %)))))))
        users (api/users)
        services (api/services)
        running (filter-by :state "running" services)
        tasks (api/tasks)
        networks (api/networks)
        volumes (api/volumes)
        stacks (api/stacks)
        nodes (filter-by :state "ready" (api/nodes))
        service (->> services (filter #(= "swarmpit/swarmpit" (get-in % [:repository :name]))) (first))
        agent (->> (api/stack (:stack service)) :services (filter #(= "swarmpit/agent" (get-in % [:repository :name]))) (first))]
    (merge (info)
           (select-keys service [:createdAt :updatedAt])
           (select-keys (:repository service) [:tag :imageDigest])
           {:autoredeploy (-> service :deployment :autoredeploy)
            :agent        (select-keys (:repository agent) [:tag :imageDigest])
            :registries   (merge registries {:total (reduce + (vals registries))})
            :users        {:total        (count users)
                           :admins       (count (filter-by :role "admin" users))
                           :api          (count (filter :api-token users))
                           :defaultAdmin (contains? (set (map :username users)) "admin")}
            :swarm        {:digest   (str "sha256:" (-> (client/swarm) :ID (hash/sha256) (bytes->hex)))
                           :stacks   {:deployed (count (filter-by :state "deployed" stacks))
                                      :inactive (count (filter-by :state "inactive" stacks))
                                      :services (count (filter :stack services))
                                      :networks (count (filter :stack networks))
                                      :volumes  (count (filter #(contains? (->> (map :stackName stacks) (set)) (:stack %))
                                                               volumes))}
                           :nodes    {:ready    (count nodes)
                                      :managers (count (filter-by :role "manager" nodes))
                                      :arch     (freq-of :arch nodes)
                                      :os       (freq-of :os nodes)
                                      :engine   (freq-of :engine nodes)}
                           :services {:deployed     (count services)
                                      :running      (count running)
                                      :command      (count (filter :command services))
                                      :healthcheck  (count (filter :healthcheck services))
                                      :sum          (zipmap stats (->> stats (map #(->> services (map %) (flatten) (count)))))
                                      :max          (zipmap stats (->> stats (map #(->> services (map %) (map count) (apply max)))))
                                      :autoredeploy (->> services (filter #(get-in % [:deployment :autoredeploy])) (count))
                                      :logDrivers   (->> services
                                                         (map #(get-in % [:logdriver :name]))
                                                         (set))}
                           :networks {:total   (count networks)
                                      :used    (->> services (map :networks) (flatten) (set) (count))
                                      :drivers (->> services
                                                    (map :networks) (flatten)
                                                    (map :driver)
                                                    (filter identity)
                                                    (set))}
                           :volumes  {:total   (count volumes)
                                      :used    (->> services (map :volumes) (flatten) (set) (count))
                                      :drivers (->> services
                                                    (map :mounts) (flatten)
                                                    (map #(get-in % [:volumeOptions :driver :name]))
                                                    (filter identity)
                                                    (set))}
                           :tasks    {:deployed (count (filter-by :desiredState "running" tasks))
                                      :running  (count (filter-by :state "running" tasks))}
                           :secrets  {:total (count (api/secrets))
                                      :used  (->> services (map :secrets) (flatten) (set) (count))}
                           :configs  {:total (count (api/configs))
                                      :used  (->> services (map :configs) (flatten) (set) (count))}}})))