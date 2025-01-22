(ns swarmpit.registry.client-test
  (:require [clojure.test :refer :all]
            [swarmpit.registry.client :refer :all]
            [swarmpit.http :as http])
  (:import (clojure.lang ExceptionInfo)
           (java.io IOException)))

(deftest registry-client-test
  (testing "dns error"
    (with-redefs [http/execute-in-scope (fn [_] (throw (ex-info "Registry failure: not-existing-addr: Name or service not known"
                                                               {:status 500
                                                                :type :http-client
                                                                :body {:error "not-existing-addr: Name or service not known"}})))]
      (is (thrown-with-msg?
            ExceptionInfo #"Registry failure: not-existing-addr: Name or service not known"
            (repositories {:url (str "http://not-existing-addr-" (swarmpit.uuid/uuid))})))))

  (testing "timeout error"
    (with-redefs [http/execute-in-scope (fn [_] (throw (ex-info "Registry error: Request timeout"
                                                               {:status 408
                                                                :type :http-client
                                                                :body {:error "Request timeout"}})))]
      (is (thrown-with-msg?
            ExceptionInfo #"Registry error: Request timeout"
            (repositories {:url (str "http://slow-registry-" (swarmpit.uuid/uuid))}))))))
