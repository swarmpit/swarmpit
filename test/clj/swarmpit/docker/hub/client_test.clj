(ns swarmpit.docker.hub.client-test
  (:require [clojure.test :refer :all]
            [swarmpit.docker.hub.client :refer :all]
            [swarmpit.config :as config]
            [swarmpit.docker.hub.client :as hub-client]
            [clojure.data.json :as json]
            [clj-http.client :as http])
  (:import (clojure.lang ExceptionInfo)))

(deftest ^:integration dockerhub-test

  (testing "error"
    (is (thrown-with-msg?
          ExceptionInfo #"User not found"
          (info {:username (swarmpit.uuid/uuid)})))))

(deftest test-login
  (testing "login with valid credentials"
    (let [user {:username "valid-username" :password "valid-password"}
          response (login user)]
      (is (= 200 (:status response)))
      (is (contains? (:body response) :token))))

  (testing "login with invalid credentials"
    (let [user {:username "invalid-username" :password "invalid-password"}]
      (is (thrown-with-msg? ExceptionInfo #"Invalid username or password"
                            (login user))))))

(deftest test-info
  (testing "get user info with valid token"
    (let [user {:username "valid-username" :token "valid-token"}
          response (info user)]
      (is (= 200 (:status response)))
      (is (contains? (:body response) :username))))

  (testing "get user info with invalid token"
    (let [user {:username "valid-username" :token "invalid-token"}]
      (is (thrown-with-msg? ExceptionInfo #"Invalid token"
                            (info user))))))

(deftest test-repositories-by-namespace
  (testing "get repositories by namespace with valid token"
    (let [token "valid-token"
          namespace "valid-namespace"
          response (repositories-by-namespace token namespace)]
      (is (= 200 (:status response)))
      (is (contains? (:body response) :results))))

  (testing "get repositories by namespace with invalid token"
    (let [token "invalid-token"
          namespace "valid-namespace"]
      (is (thrown-with-msg? ExceptionInfo #"Invalid token"
                            (repositories-by-namespace token namespace))))))

(deftest test-namespaces
  (testing "get namespaces with valid token"
    (let [token "valid-token"
          response (namespaces token)]
      (is (= 200 (:status response)))
      (is (contains? (:body response) :namespaces))))

  (testing "get namespaces with invalid token"
    (let [token "invalid-token"]
      (is (thrown-with-msg? ExceptionInfo #"Invalid token"
                            (namespaces token))))))

(deftest test-repositories
  (testing "search repositories with valid query"
    (let [query "valid-query"
          page 1
          response (repositories query page)]
      (is (= 200 (:status response)))
      (is (contains? (:body response) :results))))

  (testing "search repositories with invalid query"
    (let [query "invalid-query"
          page 1]
      (is (thrown-with-msg? ExceptionInfo #"No results found"
                            (repositories query page))))))
