(ns swarmpit.http-test
  (:require [clojure.test :refer :all]
            [swarmpit.http :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(deftest execute-in-scope-test

  (testing "scope"
    (is (thrown-with-msg?
          ExceptionInfo #"HTTP failure"
          (execute-in-scope {:method :GET
                             :url    "invalid url"})))
    (is (thrown-with-msg?
          ExceptionInfo #"Some scope failure"
          (execute-in-scope {:method :GET
                             :url    "invalid url"
                             :scope  "Some scope"})))))
