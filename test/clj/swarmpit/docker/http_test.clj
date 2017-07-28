(ns swarmpit.docker.http-test
  (:refer-clojure :exclude [get])
  (:require [clojure.test :refer :all]
            [swarmpit.docker.http :refer :all]))

(deftest ^:integration http
  (testing "execute"
    (is (some? (execute "GET" "/version" nil nil nil)))))
