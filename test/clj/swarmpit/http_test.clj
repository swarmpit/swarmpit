(ns swarmpit.http-test
  (:require [clojure.test :refer :all]
            [org.httpkit.client :as http]
            [swarmpit.http :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(deftest execute-in-scope-test

  (testing "scope"
    (is (thrown-with-msg?
          ExceptionInfo #"HTTP failure"
          (execute-in-scope @(http/get "invalid url") nil)))
    (is (thrown-with-msg?
          ExceptionInfo #"Some scope failure"
          (execute-in-scope @(http/get "invalid url") "Some scope")))))
