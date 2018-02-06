(ns swarmpit.docker.registry.client-test
  (:require [clojure.test :refer :all]
            [swarmpit.docker.registry.client :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(deftest ^:integration docker-registry-test

  (testing "error"
    (is (thrown-with-msg?
          ExceptionInfo #"Docker registry error: authentication required"
          (tags nil "nginx")))))