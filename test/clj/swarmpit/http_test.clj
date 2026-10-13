(ns swarmpit.http-test
  (:require [clojure.test :refer :all]
            [swarmpit.http :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(deftest binary-response-errors-retain-the-docker-message
  (with-redefs [swarmpit.http/req-func
                {:GET (fn [& _]
                        (throw (ex-info "Docker error"
                                        {:status 501
                                         :body (.getBytes "{\"message\":\"configured logging driver does not support reading\"}" "UTF-8")
                                         :reason-phrase "Not Implemented"})))}]
    (is (thrown-with-msg? ExceptionInfo #"configured logging driver does not support reading"
                          (execute-in-scope {:method :GET :url "http://localhost/containers/test/logs"
                                             :options {:as :byte-array} :error-handler :message})))))

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
