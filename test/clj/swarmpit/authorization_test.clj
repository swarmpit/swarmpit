(ns swarmpit.authorization-test
  (:require [clojure.test :refer :all]
            [swarmpit.authorization :as authorization]
            [swarmpit.config :as cfg]))

(defn- with-token
  [token f]
  (try
    (cfg/update! (if token {:agent-token token} {}))
    (f)
    (finally (cfg/update! {}))))

(def ^:private handled {:status 200 :body "handled"})

(def ^:private app
  (authorization/authorization-middleware (constantly handled)))

(defn- push-event
  [request]
  (app (merge {:request-method :post
               :uri            "/events"
               :remote-addr    "10.0.0.5"}
              request)))

(defn- accepted?
  [request]
  (= handled (push-event request)))

(deftest event-push-without-a-configured-token
  (testing "released agents send no credentials, so the push is accepted (#735)"
    (with-token nil
      #(is (accepted? {})))))

(deftest event-push-with-a-configured-token
  (with-token "s3cret"
    (fn []
      (testing "accepted via header"
        (is (accepted? {:headers {"x-swarmpit-agent-token" "s3cret"}})))

      (testing "accepted via query string, which is all EVENT_ENDPOINT can carry"
        (is (accepted? {:query-string "token=s3cret"}))
        (is (accepted? {:query-string "other=1&token=s3cret"})))

      (testing "rejected when missing"
        (is (not (accepted? {})))
        (is (not (accepted? {:query-string ""}))))

      (testing "rejected when wrong"
        (is (not (accepted? {:headers {"x-swarmpit-agent-token" "nope"}})))
        (is (not (accepted? {:query-string "token=nope"}))))

      (testing "a malformed query string is rejected, not a crash"
        (is (not (accepted? {:query-string "%%%"}))))

      (testing "rejection is a tagged 401"
        (let [{:keys [status headers body]} (push-event {})]
          (is (= 401 status))
          (is (= "swarmpit" (get headers "X-Backend-Server")))
          (is (= "Authentication failed" (:error body)))))

      (testing "an authenticated user is still allowed through"
        (is (accepted? {:identity {:usr {:username "admin"}}}))))))

(deftest event-push-token-is-url-decoded
  (with-token "s3/cret"
    (fn []
      (is (accepted? {:query-string "token=s3%2Fcret"}))
      (is (not (accepted? {:query-string "token=s3%2Fnope"}))))))

(deftest event-stream-stays-open-for-the-browser
  (testing "GET /events is not gated by the agent token"
    (with-token "s3cret"
      #(is (= handled (app {:request-method :get :uri "/events"}))))))
