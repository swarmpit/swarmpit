(ns swarmpit.docker.http-test
  (:refer-clojure :exclude [get])
  (:require [clojure.test :refer :all]
            [swarmpit.docker.http :refer :all])
  (:import (clojure.lang ExceptionInfo)))

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
          ExceptionInfo #"Docker failure: not-existing-dns: Name or service not known"
          (with-redefs [swarmpit.config/config {:docker-sock "http://not-existing-dns"}]
            (get "/version")))))

  (testing "invalid socket"
    (is (thrown-with-msg?
          ExceptionInfo #"Docker failure: No such file or directory"
          (with-redefs [swarmpit.config/config {:docker-sock "/not/existing/socket.sock"}]
            (get "/version"))))))
