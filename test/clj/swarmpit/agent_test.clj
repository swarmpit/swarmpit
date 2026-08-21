(ns swarmpit.agent-test
  (:require [clojure.test :refer :all]
            [swarmpit.agent :as agent]
            [swarmpit.api :as api]))

(def ^:private autoredeploy-job #'agent/autoredeploy-job)

(defn- service
  [overrides]
  (merge {:id          "svc1"
          :serviceName "app"
          :repository  {:name "nginx" :tag "latest" :imageDigest "sha256:aaa"}
          :status      {:update "completed"}
          :deployment  {:autoredeploy true}}
         overrides))

(defn- run-job
  "Run one autoredeploy tick against canned services and a canned registry
   digest, returning the redeploy calls it made."
  [services digest]
  (let [calls (atom [])]
    (with-redefs [api/services          (constantly services)
                  api/repository-digest (fn [_ _ tag] (if (fn? digest) (digest tag) digest))
                  api/redeploy-service  (fn [_ id _ d] (swap! calls conj {:id id :digest d}))]
      (autoredeploy-job))
    @calls))

(deftest autoredeploy-job-test
  (testing "redeploys when the upstream digest actually changed"
    (is (= [{:id "svc1" :digest "sha256:bbb"}]
           (run-job [(service {})] "sha256:bbb"))))

  (testing "no-op when the digest is unchanged"
    (is (empty? (run-job [(service {})] "sha256:aaa"))))

  (testing "no-op when the upstream digest cannot be resolved (#738)"
    (is (empty? (run-job [(service {})] nil)))
    (is (empty? (run-job [(service {})] ""))))

  (testing "skips services without autoredeploy"
    (is (empty? (run-job [(service {:deployment {:autoredeploy false}})] "sha256:bbb"))))

  (testing "skips services with an update already in flight (#738)"
    (is (empty? (run-job [(service {:status {:update "updating"}})] "sha256:bbb")))
    (is (empty? (run-job [(service {:status {:update "rollback_started"}})] "sha256:bbb"))))

  (testing "an untagged image resolves against latest instead of 404ing (#738)"
    (is (= [{:id "svc1" :digest "sha256:bbb"}]
           (run-job [(service {:repository {:name "nginx" :tag "" :imageDigest "sha256:aaa"}})]
                    #(when (= "latest" %) "sha256:bbb")))))

  (testing "one failing service does not abort the rest of the tick"
    (let [calls (atom [])]
      (with-redefs [api/services (constantly [(service {:id "boom"}) (service {:id "svc2"})])
                    api/repository-digest (fn [_ _ _] "sha256:bbb")
                    api/redeploy-service (fn [_ id _ d]
                                           (when (= "boom" id)
                                             (throw (ex-info "nope" {})))
                                           (swap! calls conj {:id id :digest d}))]
        (autoredeploy-job))
      (is (= [{:id "svc2" :digest "sha256:bbb"}] @calls)))))
