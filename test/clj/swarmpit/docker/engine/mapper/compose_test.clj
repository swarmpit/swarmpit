(ns swarmpit.docker.engine.mapper.compose-test
  (:require [clojure.test :refer :all]
            [swarmpit.docker.engine.mapper.compose :refer [service]]))

(defn- svc [overrides]
  (merge {:serviceName "web"
          :mode        "replicated"
          :replicas    1
          :repository  {:image "nginx:alpine"}
          :resources   {:reservation {:cpu 0 :memory 0}
                        :limit       {:cpu 0 :memory 0}}
          :deployment  {:update        {:parallelism 1 :delay 0 :order "stop-first" :failureAction "pause"}
                        :restartPolicy {:condition "any" :delay 5 :window 0 :attempts 0}
                        :placement     []}}
         overrides))

(defn- rendered [overrides]
  (-> (service nil (svc overrides)) :web))

(deftest ports-short-syntax-for-plain-tcp-ingress
  (is (= ["8080:80"]
         (:ports (rendered {:ports [{:hostPort 8080 :containerPort 80
                                     :protocol "tcp" :mode "ingress"}]})))))

(deftest ports-long-syntax-when-host-mode
  (let [[p] (:ports (rendered {:ports [{:hostPort 80 :containerPort 80
                                        :protocol "tcp" :mode "host"}]}))]
    (is (= 80 (:target p)))
    (is (= 80 (:published p)))
    (is (= "host" (:mode p)))
    (is (not (contains? p :protocol)))))

(deftest ports-long-syntax-when-udp
  (let [[p] (:ports (rendered {:ports [{:hostPort 53 :containerPort 53
                                        :protocol "udp" :mode "ingress"}]}))]
    (is (= "udp" (:protocol p)))
    (is (not (contains? p :mode)))))

(deftest ports-long-syntax-when-host-and-udp
  (let [[p] (:ports (rendered {:ports [{:hostPort 53 :containerPort 53
                                        :protocol "udp" :mode "host"}]}))]
    (is (= "udp" (:protocol p)))
    (is (= "host" (:mode p)))))
