(ns swarmpit.api-test
  (:require [clojure.test :refer :all]
            [digest :refer [digest]]
            [swarmpit.api :refer :all]
            [swarmpit.config :as cfg]
            [swarmpit.couchdb.mapper.outbound :refer [->password]]))

(deftest password-check-test
  (let [pass "heslo"
        hashed (->password pass)]

    (testing "speed"
      (cfg/update! {:password-hashing
                    {:alg :pbkdf2+sha512 :iterations 100000}})
      (println "Evaluating baseline" (cfg/config :password-hashing))
      (time (is (some? (->password pass))))
      (cfg/update! {})
      (println "Evaluating defaults" (cfg/config :password-hashing))
      (time (is (some? (->password pass)))))

    (testing "check"
      (is (true? (password-check pass hashed))))

    (testing "upgrade from old hash"
      (let [old-hash (digest "sha-256" pass)
            new-hash (atom nil)]
        (is (thrown? Exception (password-check pass old-hash)))
        (is (false? (password-check-upgrade pass "heslo" nil)))
        (is (true? (password-check-upgrade pass old-hash
                                           #(reset! new-hash hashed))))
        (is (some? @new-hash))
        (is (true? (password-check pass @new-hash)))
        (is (true? (password-check-upgrade pass @new-hash nil)))))))
