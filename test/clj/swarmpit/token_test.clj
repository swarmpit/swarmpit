(ns swarmpit.token-test
  (:require [clojure.test :refer :all]
            [clj-time.core :as t]
            [swarmpit.token :as token]))

(deftest claim-default-expiry-is-about-one-day
  (let [c (token/claim {:username "alice" :role "user"})
        exp (:exp c)
        iat (:iat c)]
    (is (some? exp))
    (is (t/after? exp (t/plus iat (t/hours 23)))
        "exp should be at least 23 hours after iat")
    (is (t/before? exp (t/plus iat (t/hours 25)))
        "exp should be at most 25 hours after iat")))

(deftest claim-allows-exp-override
  (let [c (token/claim {:username "alice"} {:exp nil})]
    (is (nil? (:exp c))
        "non-expiring (api) tokens should allow nil exp")))

(deftest claim-has-jti-and-iss
  (let [c (token/claim {:username "alice"})]
    (is (string? (:jti c)))
    (is (= "swarmpit" (:iss c)))))

(deftest claim-iss-override-for-api-tokens
  (let [c (token/claim {:username "alice"} {:iss "swarmpit-api"})]
    (is (= "swarmpit-api" (:iss c)))))
