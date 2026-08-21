(ns swarmpit.swagger-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [cheshire.core :as json]
            [spec-tools.data-spec :as ds]
            [swarmpit.couchdb.client :as cc]
            [swarmpit.routes-spec :as spec]
            [swarmpit.server :as server]))

(defn- swagger
  []
  (with-redefs [cc/get-secret (constantly {:secret "test"})]
    (-> (server/app {:request-method :get :uri "/api/swagger.json"})
        :body
        io/reader
        slurp
        (json/parse-string))))

(def ^:private paths (delay (get (swagger) "paths")))

(deftest account-endpoints-are-documented
  (testing "the endpoints the UI uses to manage credentials are in swagger (#743)"
    (is (contains? (get @paths "/password") "post"))
    (is (contains? (get @paths "/api-token") "post"))
    (is (contains? (get @paths "/api-token") "delete")))

  (testing "password change documents its body, so it doesn't read as immutable"
    (let [body (->> (get-in @paths ["/password" "post" "parameters"])
                    (filter #(= "body" (get % "in")))
                    first
                    (#(get % "schema")))]
      (is (= #{"password" "new-password" "confirm-password"}
             (set (keys (get body "properties")))))
      (is (= #{"password" "new-password"} (set (get body "required")))
          "confirm-password is ignored server-side, so it must not be required")))

  (testing "password change documents the wrong-password response"
    (is (= #{"200" "403"} (set (keys (get-in @paths ["/password" "post" "responses"]))))))

  (testing "api-token generation documents that it returns a token"
    (let [schema (get-in @paths ["/api-token" "post" "responses" "200" "schema"])]
      (is (= ["token"] (get schema "required")))
      (is (contains? (get schema "properties") "expiresAt")))))

(deftest swagger-still-describes-the-rest-of-the-api
  (testing "adding the account routes did not drop anything"
    (is (< 55 (count @paths)))
    (is (contains? @paths "/api/services"))
    (is (contains? @paths "/api/stacks"))))

(deftest password-change-spec-matches-what-the-ui-sends
  (let [pw (ds/spec ::password-change spec/password-change)]
    (testing "the payload the UI posts is accepted"
      (is (s/valid? pw {:password "old" :new-password "new" :confirm-password "new"})))

    (testing "confirm-password is optional for api clients"
      (is (s/valid? pw {:password "old" :new-password "new"})))

    (testing "both passwords are required"
      (is (not (s/valid? pw {:password "old"})))
      (is (not (s/valid? pw {:new-password "new"})))
      (is (not (s/valid? pw {}))))))
