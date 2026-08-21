(ns swarmpit.docker.engine.mapper.healthcheck-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [spec-tools.data-spec :as ds]
            [swarmpit.routes-spec :as spec]
            [swarmpit.utils :refer [->nano nano-> max-nano-seconds]]
            [swarmpit.docker.engine.mapper.inbound :as inbound]
            [swarmpit.docker.engine.mapper.outbound :as outbound]))

(def ^:private healthcheck-spec (ds/spec ::healthcheck spec/healthcheck))

(deftest nano-conversion-test
  (testing "seconds convert to nanoseconds"
    (is (= 30000000000 (->nano 30)))
    (is (nil? (->nano nil))))

  (testing "absurd input promotes instead of throwing integer overflow (#740)"
    (is (= 30000000000000000000N (->nano 30000000000))))

  (testing "nanoseconds convert back to whole seconds, never a ratio"
    (is (= 30 (nano-> 30000000000)))
    (is (= 1.5 (nano-> 1500000000)))
    (is (nil? (nano-> nil)))))

(deftest healthcheck-spec-test
  (testing "durations in seconds are accepted"
    (is (s/valid? healthcheck-spec {:test        ["CMD" "wget" "-qO-" "http://localhost:3000/health"]
                                    :interval    30
                                    :timeout     5
                                    :startPeriod 120
                                    :retries     3})))

  (testing "every field is optional"
    (is (s/valid? healthcheck-spec {}))
    (is (s/valid? healthcheck-spec {:test ["CMD" "true"] :retries 3})))

  (testing "durations that would overflow int64 nanoseconds are rejected (#740)"
    (is (not (s/valid? healthcheck-spec {:interval 30000000000})))
    (is (not (s/valid? healthcheck-spec {:startPeriod 120000000000})))
    (is (s/valid? healthcheck-spec {:interval max-nano-seconds}))
    (is (not (s/valid? healthcheck-spec {:interval (inc max-nano-seconds)}))))

  (testing "the payload from #740 is rejected as a whole rather than 500ing"
    (is (not (s/valid? healthcheck-spec {:test        ["CMD" "wget" "-qO-" "http://localhost:3000/health"]
                                         :interval    30000000000
                                         :timeout     5000000000
                                         :startPeriod 120000000000
                                         :retries     3}))))

  (testing "negative durations are rejected"
    (is (not (s/valid? healthcheck-spec {:interval -1})))))

(deftest healthcheck-roundtrip-test
  (let [domain {:test        ["CMD" "true"]
                :interval    30
                :timeout     5
                :startPeriod 120
                :retries     3}]
    (testing "outbound writes StartPeriod (#740)"
      (is (= {:Test        ["CMD" "true"]
              :Interval    30000000000
              :Timeout     5000000000
              :StartPeriod 120000000000
              :Retries     3}
             (outbound/->service-healthcheck domain))))

    (testing "inbound reads StartPeriod back"
      (is (= domain
             (-> domain
                 (outbound/->service-healthcheck)
                 (inbound/->service-healthcheck)))))

    (testing "nil healthcheck stays nil both ways"
      (is (nil? (outbound/->service-healthcheck nil)))
      (is (nil? (inbound/->service-healthcheck nil))))))
