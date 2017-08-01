(ns swarmpit.dockerregistry.client-test
  (:require [clojure.test :refer :all]
            [swarmpit.dockerregistry.client :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(deftest ^:integration dockerregistry-test

  (testing "error"
    (is (thrown-with-msg?
          ExceptionInfo #"Docker registry error: authentication required"
          (tags nil "nginx")))))
