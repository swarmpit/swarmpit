(ns swarmpit.docker.http-test
  (:refer-clojure :exclude [get])
  (:require [clojure.test :refer :all]
            [swarmpit.test :refer :all]
            [swarmpit.docker.http :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(use-fixtures :once dind-socket-fixture)

(deftest ^:integration http
  (testing "execute"
    (let [response (execute "GET" "/version" nil nil nil)]
      (is (some? response))
      (is (= response (get "/version")))))

  (testing "object not found"
    (is (thrown-with-msg?
          ExceptionInfo #"not-existing-service"
          (get "/services/not-existing-service"))))

  (testing "page not found"
    (is (thrown-with-msg?
          ExceptionInfo #"Docker error: page not found"
          (get "/not-existing-page"))))

  (testing "connection refused"
    (is (thrown-with-msg?
          ExceptionInfo #"Connection refused"
          (with-redefs [swarmpit.config/config {:docker-sock "http://localhost:23095"}]
            (get "/version")))))

  (testing "invalid address"
    (is (thrown-with-msg?
          ExceptionInfo #"Docker failure: not-existing-dns:"
          (with-redefs [swarmpit.config/config {:docker-sock "http://not-existing-dns"}]
            (get "/version")))))

  (testing "timeout"
    (is (thrown?
          ExceptionInfo #"Docker error: Request timeout"
          (with-redefs [swarmpit.docker.http/timeout 0]
            (get "/services")))))

  (testing "invalid socket"
    (is (thrown-with-msg?
          ExceptionInfo #"Docker failure: No such file or directory"
          (with-redefs [swarmpit.config/config {:docker-sock "/not/existing/socket.sock"}]
            (get "/version"))))))
