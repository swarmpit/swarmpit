(ns swarmpit.api-test
  (:require [clojure.test :refer :all]
            [swarmpit.test :refer :all]
            [swarmpit.api :refer :all]))

(use-fixtures :once dind-socket-fixture running-service-fixture)

(deftest ^:integration docker
  (let [service-id (-> (swarmpit.docker.client/services) first :ID)]

    (testing "services"
      (is (some? (services))))

    (testing "service"
      (is (some? (service service-id)))
      (is (= service-id (:id (service service-id))))
      (is (= (-> (services) first) (service service-id)))
      (is (some? (service-networks service-id)))
      (is (some? (service-tasks service-id))))

    (testing "secrets"
      (is (some? (secrets))))

    (testing "networks"
      (let [networks (networks)
            some-network (-> networks first)]
        (is (some? networks))
        (is (some? (:id some-network)))
        ; TODO: following assert should be fixed, there are no reason that date in list should be different
        (is (not (= (:created some-network) (:created (network (:id some-network))))))
        (is (= "overlay" (:driver some-network)))
        (is (= "swarm" (:scope some-network)))))

    (testing "volumes"
      (is (some? (volumes))))

    (testing "nodes"
      (let [nodes (nodes)
            some-node (-> nodes first)]
        (is (some? nodes))
        (is (some? (:id some-node)))
        (is (= some-node (node (:id some-node))))
        (is (= "manager" (:role some-node)))
        (is (= "ready" (:state some-node)))))

    (testing "tasks"
      (let [tasks (tasks)
            some-task (-> tasks first)]
        (is (some? tasks))
        (is (= (service-tasks service-id)
               (->> tasks (filter #(.startsWith (:taskName %) "test")))))
        (is (= some-task (task (:id some-task))))))))