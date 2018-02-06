(ns swarmpit.docker.hub.client-test
  (:require [clojure.test :refer :all]
            [swarmpit.docker.hub.client :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(deftest ^:integration dockerhub-test

  (testing "error"
    (is (thrown-with-msg?
          ExceptionInfo #"Dockerhub error: Not Found"
          (info {:username (swarmpit.uuid/uuid)})))))
