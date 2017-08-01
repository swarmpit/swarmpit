(ns swarmpit.dockerhub.client-test
  (:require [clojure.test :refer :all]
            [swarmpit.dockerhub.client :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(deftest ^:integration dockerhub-test

  (testing "error"
    (is (thrown-with-msg?
          ExceptionInfo #":password"
          (login {:username "fail" :password "pass"})))
    (is (thrown-with-msg?
          ExceptionInfo #"Dockerhub error: Incorrect authentication credentials."
          (login {:username "fail" :password "incorrect"})))))
