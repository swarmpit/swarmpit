(ns swarmpit.registry.client-test
  (:require [clojure.test :refer :all]
            [swarmpit.registry.client :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(deftest registry-client-test

  (testing "dns error"
    (is (thrown-with-msg?
          ExceptionInfo #"Registry failure: .*"
          (repositories {:url (str "http://not-existing-addr-" (swarmpit.uuid/uuid))})))))