(ns swarmpit.authentication-test
  (:require [clojure.test :refer :all]
            [swarmpit.authentication :as authentication]
            [swarmpit.couchdb.client :as cc]))

(def ^:private secret-cache @#'cc/secret-cache)

(def ^:private handled {:status 200 :body "handled"})

(defn- app
  [calls]
  (authentication/authentication-middleware
    (fn [_] (swap! calls inc) handled)))

(defn- with-clear-cache
  [f]
  (try
    (reset! secret-cache nil)
    (f)
    (finally (reset! secret-cache nil))))

(use-fixtures :each with-clear-cache)

(deftest secret-is-cached-not-queried-per-request
  (testing "an immutable secret is read once, not on every request (#706)"
    (let [reads (atom 0)]
      (with-redefs [cc/get-secret (fn [] (swap! reads inc) {:secret "s3cret"})]
        (is (= "s3cret" (cc/secret)))
        (is (= "s3cret" (cc/secret)))
        (is (= "s3cret" (cc/secret))))
      (is (= 1 @reads)))))

(deftest secret-failure-is-not-cached
  (testing "a db outage does not poison the cache, so it recovers on its own"
    (let [up (atom false)]
      (with-redefs [cc/get-secret (fn []
                                    (if @up
                                      {:secret "s3cret"}
                                      (throw (ex-info "DB error: not_found" {:status 404}))))]
        (is (thrown? Exception (cc/secret)))
        (reset! up true)
        (is (= "s3cret" (cc/secret))))))

  (testing "an empty db is not cached either"
    (reset! secret-cache nil)
    (let [doc (atom nil)]
      (with-redefs [cc/get-secret (fn [] @doc)]
        (is (nil? (cc/secret)))
        (reset! doc {:secret "s3cret"})
        (is (= "s3cret" (cc/secret)))))))

(deftest unreachable_secret_is_503_not_500
  (testing "a db error while reading the secret is a clean 503 (#706)"
    (let [calls (atom 0)
          handler (app calls)]
      (with-redefs [cc/get-secret (fn [] (throw (ex-info "DB error: not_found" {:status 404})))]
        (let [{:keys [status headers body]} (handler {:request-method :post :uri "/events"})]
          (is (= 503 status))
          (is (= "swarmpit" (get headers "X-Backend-Server")))
          (is (= "Database unavailable" (:error body)))))
      (is (zero? @calls) "the request must not reach the handler without a secret")))

  (testing "a missing secret document is also a 503"
    (let [calls (atom 0)
          handler (app calls)]
      (with-redefs [cc/get-secret (constantly nil)]
        (is (= 503 (:status (handler {:request-method :get :uri "/version"})))))
      (is (zero? @calls)))))

(deftest requests_pass_through_when_the_secret_is_available
  (let [calls (atom 0)
        handler (app calls)]
    (with-redefs [cc/get-secret (constantly {:secret "s3cret"})]
      (testing "an anonymous request is handed to the handler"
        (is (= handled (handler {:request-method :get :uri "/version"})))
        (is (= 1 @calls)))

      (testing "a garbage token is a tagged 401, not a 500"
        (let [{:keys [status headers body]} (handler {:request-method :get
                                                      :uri            "/api/services"
                                                      :headers        {"authorization" "Bearer not-a-jwt"}})]
          (is (= 401 status))
          (is (= "swarmpit" (get headers "X-Backend-Server")))
          (is (= "Token invalid" (:error body))))))))
