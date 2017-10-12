(ns swarmpit.http-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as http]
            [swarmpit.http :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(deftest execute-in-scope-test

  (testing "scope"
    (is (thrown-with-msg?
          ExceptionInfo #"HTTP failure"
          (execute-in-scope {:call-fx #(http/get "invalid url")})))
    (is (thrown-with-msg?
          ExceptionInfo #"Some scope failure"
          (execute-in-scope {:call-fx #(http/get "invalid url")
                             :scope   "Some scope"})))))
