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
                          (delete-secret "not-existing")))))