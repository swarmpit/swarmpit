(ns swarmpit.api-test
  (:require [clojure.test :refer :all]
            [swarmpit.test :refer :all]
            [swarmpit.api :refer :all]
            [clojure.edn :as edn]))

(use-fixtures :once dind-socket-fixture running-service-fixture db-init-fixture)

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

    (testing "crud service"
      (let [id (-> (edn/read-string (slurp "test/clj/swarmpit/create-service.edn"))
                   (create-service)
                   :id)
            created (service id)]
        (is (some? id))
        (is (some? created))
        (is (= "nginx" (-> created :repository :name)))
        (is (= 1 (-> created :replicas)))
        (update-service id (merge created {:replicas 2}) false)
        (is (= 2 (-> id (service) :replicas)))
        (delete-service id)
        (is (empty? (->> (services) (filter #(= id (:id %))))))))

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
        (is (= some-task (task (:id some-task))))))

    (testing "find public repository"
      (let [results (dockerhub-repositories "nginx" 1)]
        (is (some? results))
        (is (= "nginx" (:query results)))
        (is (= "nginx" (-> results :results first :name)))))))