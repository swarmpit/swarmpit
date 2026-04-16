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

(deftest volumes-tmpfs-long-syntax
  (let [[v] (:volumes (rendered {:mounts [{:type "tmpfs"
                                           :containerPath "/var/cache/nginx"}]}))]
    (is (= "tmpfs" (:type v)))
    (is (= "/var/cache/nginx" (:target v)))
    (is (not (contains? v :read_only)))))

(deftest volumes-tmpfs-read-only
  (let [[v] (:volumes (rendered {:mounts [{:type "tmpfs"
                                           :containerPath "/run"
                                           :readOnly true}]}))]
    (is (true? (:read_only v)))))

(deftest volumes-bind-keeps-short-syntax
  (is (= ["/data:/data"]
         (:volumes (rendered {:mounts [{:type "bind"
                                        :host "/data"
                                        :containerPath "/data"}]})))))

(deftest volumes-bind-ro-keeps-short-syntax
  (is (= ["/data:/data:ro"]
         (:volumes (rendered {:mounts [{:type "bind"
                                        :host "/data"
                                        :containerPath "/data"
                                        :readOnly true}]})))))

(deftest secrets-short-when-source-equals-target
  (is (= ["db_password"]
         (:secrets (rendered {:secrets [{:secretName "db_password"
                                         :secretTarget "db_password"}]})))))

(deftest secrets-long-with-custom-target
  (let [[s] (:secrets (rendered {:secrets [{:secretName "db_password"
                                            :secretTarget "/run/secrets/db"}]}))]
    (is (= "db_password" (:source s)))
    (is (= "/run/secrets/db" (:target s)))))

(deftest secrets-long-with-mode
  (let [[s] (:secrets (rendered {:secrets [{:secretName "db_password"
                                            :secretTarget "db_password"
                                            :uid "103" :gid "103" :mode 0440}]}))]
    (is (= "db_password" (:source s)))
    (is (not (contains? s :target)))
    (is (= "103" (:uid s)))
    (is (= "103" (:gid s)))
    (is (= 0440 (:mode s)))))

(deftest configs-long-with-mode
  (let [[c] (:configs (rendered {:configs [{:configName "my_config"
                                            :configTarget "/redis_config"
                                            :mode 0440}]}))]
    (is (= "my_config" (:source c)))
    (is (= "/redis_config" (:target c)))
    (is (= 0440 (:mode c)))))

(deftest command-as-array-preserved
  (is (= ["python" "-u" "app.py"]
         (:command (rendered {:command ["python" "-u" "app.py"]})))))

(deftest command-as-string-preserved
  (is (= "redis-server --appendonly yes"
         (:command (rendered {:command "redis-server --appendonly yes"})))))

(deftest hostname-preserved
  (is (= "rabbit-{{.Task.Slot}}"
         (:hostname (rendered {:hostname "rabbit-{{.Task.Slot}}"})))))

(deftest entrypoint-preserved
  (is (= ["python" "-u"]
         (:entrypoint (rendered {:entrypoint ["python" "-u"]})))))

(deftest entrypoint-and-command-independent
  (let [r (rendered {:entrypoint ["python"] :command ["app.py"]})]
    (is (= ["python"] (:entrypoint r)))
    (is (= ["app.py"] (:command r)))))

(deftest isolation-preserved
  (is (= "hyperv" (:isolation (rendered {:isolation "hyperv"})))))

(deftest sysctls-preserved
  (is (= {:net.ipv6.conf.lo.disable_ipv6 "0"
          :net.ipv6.conf.all.disable_ipv6 "0"}
         (:sysctls (rendered {:sysctls [{:name "net.ipv6.conf.lo.disable_ipv6" :value "0"}
                                        {:name "net.ipv6.conf.all.disable_ipv6" :value "0"}]})))))

(deftest max-replicas-per-node-preserved
  (is (= 1 (get-in (rendered {:deployment {:update        {:parallelism 1 :delay 0 :order "stop-first" :failureAction "pause"}
                                           :restartPolicy {:condition "any" :delay 5 :window 0 :attempts 0}
                                           :placement     []
                                           :maxReplicas   1}})
                   [:deploy :placement :max_replicas_per_node]))))
