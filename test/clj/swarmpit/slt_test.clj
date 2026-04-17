(ns swarmpit.slt-test
  (:require [clojure.test :refer :all]
            [clojure.core.cache :as cache]
            [swarmpit.slt :as slt]))

(defn- with-fresh-cache [f]
  (let [original @#'slt/cache]
    (try
      (reset! original (cache/ttl-cache-factory {} :ttl 10000))
      (f)
      (finally
        (reset! original (cache/ttl-cache-factory {} :ttl 10000))))))

(use-fixtures :each with-fresh-cache)

(deftest create-returns-valid-slt
  (let [token (slt/create "alice")]
    (is (string? token))
    (is (slt/valid? token))
    (is (= "alice" (slt/user token)))))

(deftest consume-returns-user-and-invalidates
  (let [token (slt/create "alice")]
    (is (= "alice" (slt/consume! token)))
    (is (not (slt/valid? token))
        "token must be invalid after consume")
    (is (nil? (slt/consume! token))
        "consume on already-consumed token returns nil")))

(deftest consume-unknown-token-returns-nil
  (is (nil? (slt/consume! "no-such-token")))
  (is (nil? (slt/consume! nil)))
  (is (nil? (slt/consume! ""))))

(deftest separate-tokens-do-not-affect-each-other
  (let [a (slt/create "alice")
        b (slt/create "bob")]
    (is (= "alice" (slt/consume! a)))
    (is (= "bob" (slt/consume! b))
        "bob's token still valid after alice's consume")))
