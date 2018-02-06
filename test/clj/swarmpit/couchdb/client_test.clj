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
            ExceptionInfo #"DB failure: invalid-url: .*"
            (db-version)))))

  (testing "404 error swallowing"
    (is (nil? (dockeruser "not-existing-user")))))
