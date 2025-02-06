(ns swarmpit.docker.engine.client-test
  (:require [clojure.test :refer :all]
            [swarmpit.test :refer :all]
            [clojure.edn :as edn]
            [swarmpit.docker.engine.client :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(use-fixtures :once dind-socket-fixture)

(def service-def (edn/read-string (slurp "test/clj/swarmpit/docker/engine/service.edn")))

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
    (let [service (create-service nil service-def)]
      (is (some? service))
      (delete-service (:ID service))
      (is (empty? (->> (services)
                       (filter #(= (:ID service) (:ID %))))))))

  (testing "errors"
    (is (thrown-with-msg? ExceptionInfo #"not-existing"
                          (delete-secret "not-existing"))))

  (testing "create and delete network"
    (let [network (create-network {:Name "test-network"})]
      (is (some? network))
      (delete-network (:Id network))
      (is (empty? (->> (networks)
                       (filter #(= (:Id network) (:Id %))))))))

  (testing "create and delete volume"
    (let [volume (create-volume {:Name "test-volume"})]
      (is (some? volume))
      (delete-volume (:Name volume))
      (is (empty? (->> (volumes)
                       (filter #(= (:Name volume) (:Name %))))))))

  (testing "create and delete secret"
    (let [secret (create-secret {:Name "test-secret" :Data "dGVzdA=="})]
      (is (some? secret))
      (delete-secret (:ID secret))
      (is (empty? (->> (secrets)
                       (filter #(= (:ID secret) (:ID %))))))))

  (testing "create and delete config"
    (let [config (create-config {:Name "test-config" :Data "dGVzdA=="})]
      (is (some? config))
      (delete-config (:ID config))
      (is (empty? (->> (configs)
                       (filter #(= (:ID config) (:ID %))))))))
