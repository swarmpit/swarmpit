(ns swarmpit.docker.client-test
  (:require [clojure.test :refer :all]
            [swarmpit.test :refer :all]
            [clojure.edn :as edn]
            [swarmpit.docker.client :refer :all]))

(use-fixtures :once dind-socket-fixture)

(def service-def (edn/read-string (slurp "test/clj/swarmpit/docker/service.edn")))

(deftest ^:integration docker-client

  (testing "version"
    (is (some? (version))))

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
      (delete-service (:ID service))
      (is (empty? (->> (services)
                       (filter #(= (:ID service) (:ID %)))))))))