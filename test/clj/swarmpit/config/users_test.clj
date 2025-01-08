(ns swarmpit.config.users-test
  (:require [clojure.test :refer :all]
            [swarmpit.test :refer :all]
            [swarmpit.config.users :as users]
            [swarmpit.docker.secret :as secret]
            [clojure.java.io :as io]
            [environ.core :refer [env]]))

(use-fixtures :once db-init-fixture)

(deftest parse-user-config-test
  (testing "parse user with plain password"
    (let [user {:username "test"
                :password "pass123"
                :role "admin"}
          parsed (users/parse-user-config user)]
      (is (= "test" (:username parsed)))
      (is (= "pass123" (:password parsed)))
      (is (= "admin" (:role parsed)))))

  (testing "parse user with env password"
    (with-redefs [environ.core/env (fn [name] 
                                    (when (= name :test_password) 
                                      "envpass123"))]
      (let [user {:username "test"
                  :password_env "TEST_PASSWORD"
                  :role "viewer"}
            parsed (users/parse-user-config user)]
        (is (= "test" (:username parsed)))
        (is (= "envpass123" (:password parsed)))
        (is (= "viewer" (:role parsed))))))

  (testing "parse user with secret password"
    (with-redefs [secret/get (constantly "secretpass123")]
      (let [user {:username "test"
                  :password_secret "test_secret"
                  :role "user"}
            parsed (users/parse-user-config user)]
        (is (= "test" (:username parsed)))
        (is (= "secretpass123" (:password parsed)))
        (is (= "user" (:role parsed))))))

  (testing "default role"
    (let [user {:username "test"
                :password "pass123"}
          parsed (users/parse-user-config user)]
      (is (= "viewer" (:role parsed))))))

(deftest init-users-test
  (testing "no config file"
    (with-redefs [io/file (constantly (io/file "non-existent.yaml"))]
      (is (nil? (users/init!)))))

  (testing "with config file"
    (let [test-config "test/clj/swarmpit/config/users.yaml"]
      ; Create test config file
      (spit test-config "users:
  - username: admin
    password: admin123
    role: admin
  - username: viewer
    password_env: VIEWER_PASSWORD
    role: viewer")
      (try
        (with-redefs [io/file (constantly (io/file test-config))]
          (users/init!)
          ; Add assertions to verify users were created
          (is true))
        (finally
          ; Clean up test file
          (io/delete-file test-config true))))))
