(ns swarmpit.docker.client_test
  (:require [clojure.test :refer :all]
            [swarmpit.docker.client :refer :all]))

(use-fixtures :once
              (fn [test]
                (swarmpit.config/update! {:docker-sock "http://localhost:12375"})
                (test)))

(def service-def
  {:Name "alpine",
   :Labels {:swarmpit.service.deployment.autoredeploy "false", :swarmpit.service.registry.name "dockerhub", :swarmpit.service.registry.user nil},
   :TaskTemplate {:ContainerSpec {:Image "alpine:latest", :Mounts [], :Secrets [], :Env []},
                  :RestartPolicy {:Condition "any", :Delay 5000000000, :MaxAttempts 0},
                  :Placement {:Constraints []}, :ForceUpdate nil, :Networks []},
   :Mode {:Replicated {:Replicas 1}},
   :UpdateConfig {:Parallelism 1, :Delay 0, :FailureAction "pause"},
   :RollbackConfig {:Parallelism 1, :Delay 0, :FailureAction "pause"},
   :EndpointSpec {:Ports []}})

(deftest ^:integration docker-client

  (testing "get services"
    (is (some? (services))))

  (testing "get networks"
    (is (some? (networks))))

  (testing "get volumes"
    (is (some? (volumes))))

  (testing "get secrets"
    (is (some? (secrets))))

  (testing "get nodes"
    (is (some? (nodes))))

  (testing "get tasks"
    (is (some? (tasks))))

  (testing "create and delete service"
    (let [service (create-service service-def)]
      (is (some? service))
      (is (some? (delete-service (service :ID)))))))


