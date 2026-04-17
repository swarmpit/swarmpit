(ns swarmpit.token.blacklist-test
  (:require [clojure.test :refer :all]
            [clojure.core.cache :as cache]
            [swarmpit.token.blacklist :as bl]))

(defn- with-fresh-store [f]
  (let [s @#'bl/store]
    (try
      (reset! s (cache/ttl-cache-factory {} :ttl 3600000))
      (f)
      (finally
        (reset! s (cache/ttl-cache-factory {} :ttl 3600000))))))

(use-fixtures :each with-fresh-store)

(deftest unknown-jti-is-not-revoked
  (is (not (bl/revoked? "never-seen")))
  (is (not (bl/revoked? nil)))
  (is (not (bl/revoked? ""))))

(deftest revoke-marks-jti-as-revoked
  (bl/revoke! "jti-123")
  (is (bl/revoked? "jti-123")))

(deftest revoke-nil-is-noop
  (bl/revoke! nil)
  (is (not (bl/revoked? nil))))

(deftest revoke-only-affects-given-jti
  (bl/revoke! "alpha")
  (is (bl/revoked? "alpha"))
  (is (not (bl/revoked? "beta"))))
