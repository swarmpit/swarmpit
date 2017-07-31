(ns swarmpit.couchdb.client-test
  (:require [clojure.test :refer :all]
            [swarmpit.test :refer :all]
            [swarmpit.couchdb.client :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(use-fixtures :once db-init-fixture)

(deftest ^:integration db-client
  (testing "version"
    (is (some? (db-version))))

  (testing "some? db"
    (is (not (empty? (users))))
    (is (some? (get-secret))))

  (testing "dns error"
    (with-redefs [swarmpit.config/config {:db-url "http://invalid-url:23333"}]
      (is (thrown-with-msg?
            ExceptionInfo #"DB failure: invalid-url: Name or service not known"
            (db-version)))))

  (testing "404 error"
    (is (thrown-with-msg?
          ExceptionInfo #"DB error: not_found"
          (docker-user "not-existing-user")))))
