(ns swarmpit.docker.auth.client-test
  (:require [clojure.test :refer :all]
            [swarmpit.docker.auth.client :refer :all])
  (:import (clojure.lang ExceptionInfo)))

;(deftest ^:integration dockerauth-test
;
;  (testing "invalid login"
;    (is (thrown-with-msg?
;          ExceptionInfo #"Docker auth error: incorrect username or password"
;          (token {:username "fail" :password "fail"} "nginx")))))
