(ns swarmpit.docker.engine.http-test
  (:refer-clojure :exclude [get])
  (:require [clojure.test :refer :all]
            [swarmpit.test :refer :all]
            [swarmpit.docker.engine.http :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(use-fixtures :once dind-socket-fixture)

(deftest ^:integration http
  (testing "execute"
    (let [response (-> (execute {:method :GET
                                 :api    "/version"})
                       :body)]
      (is (some? response))))

  (testing "object not found"
    (is (thrown-with-msg?
          ExceptionInfo #"not-existing-service"
          (-> (execute {:method :GET
                        :api    "/services/not-existing-service"})
              :body))))

  (testing "page not found"
    (is (thrown-with-msg?
          ExceptionInfo #"Docker error: page not found"
          (-> (execute {:method :GET
                        :api    "/not-existing-page"})
              :body))))

  (testing "connection refused"
    (is (thrown-with-msg?
          ExceptionInfo #"Connection refused"
          (with-redefs [swarmpit.config/config {:docker-sock "http://localhost:23095"}]
            (-> (execute {:method :GET
                          :api    "/version"})
                :body)))))

  (testing "invalid address"
    (is (thrown-with-msg?
          ExceptionInfo #"Docker failure: not-existing-dns:"
          (with-redefs [swarmpit.config/config {:docker-sock "http://not-existing-dns"}]
            (-> (execute {:method :GET
                          :api    "/version"})
                :body)))))

  (testing "timeout"
    (is (thrown?
          ExceptionInfo #"Docker error: Request timeout"
          (with-redefs [swarmpit.config/config {:docker-sock         "http://localhost:12375"
                                                :docker-http-timeout 0}]
            (-> (execute {:method :GET
                          :api    "/services"})
                :body)))))

  (testing "invalid socket"
    (is (thrown-with-msg?
          ExceptionInfo #"Docker failure: No such file or directory"
          (with-redefs [swarmpit.config/config {:docker-sock "/not/existing/socket.sock"}]
            (-> (execute {:method :GET
                          :api    "/version"})
                :body))))))
