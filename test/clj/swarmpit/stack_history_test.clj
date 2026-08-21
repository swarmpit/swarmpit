(ns swarmpit.stack-history-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string]
            [cheshire.core :as json]
            [swarmpit.api :as api]
            [swarmpit.couchdb.client :as cc]
            [swarmpit.handler :as handler]
            [swarmpit.server :as server]))

(defn- swagger-paths
  []
  (with-redefs [cc/get-secret (constantly {:secret "test"})]
    (-> (server/app {:request-method :get :uri "/api/swagger.json"})
        :body io/reader slurp (json/parse-string) (get "paths"))))

(deftest history-endpoint-is-documented
  (is (contains? (get (swagger-paths) "/api/stacks/{name}/history") "get")))

(deftest stack-file-does-not-carry-history
  (testing "history is served separately, not bundled into every stackfile read"
    (with-redefs [api/stackfile (constantly {:name    "app"
                                             :spec    {:compose "a"}
                                             :history [{:at "t" :spec {:compose "a"}}]})]
      (let [body (:body (handler/stack-file {:parameters {:path {:name "app"}}}))]
        (is (= "app" (:name body)))
        (is (not (contains? body :history)))))))

(deftest append-history
  (let [saved (atom nil)
        stackfile (atom {:name "app" :history []})]
    (with-redefs [cc/stackfile (fn [_] @stackfile)
                  cc/update-stackfile (fn [_ delta]
                                        (reset! saved delta)
                                        (swap! stackfile merge delta))]

      (testing "records an entry with the trigger that caused it"
        (with-redefs [api/stack-compose (constantly "version: '3'")]
          (api/append-history! "app" {:by "bob" :trigger {:kind "stack-update"}})
          (let [entry (last (:history @saved))]
            (is (= "bob" (:by entry)))
            (is (= {:kind "stack-update"} (:trigger entry)))
            (is (= "version: '3'" (get-in entry [:spec :compose])))
            (is (string? (:at entry))))))

      (testing "records an optional comment, collapsed to one line"
        (with-redefs [api/stack-compose (constantly "compose-with-comment")]
          (api/append-history! "app" {:by      "bob"
                                      :comment "  bump alpine\n  to 3.20  "
                                      :trigger {:kind "stack-update"}})
          (is (= "bump alpine to 3.20" (:comment (last (:history @saved)))))))

      (testing "omits the comment key when blank"
        (with-redefs [api/stack-compose (constantly "compose-blank-comment")]
          (api/append-history! "app" {:by "bob" :comment "   " :trigger {:kind "stack-update"}})
          (is (not (contains? (last (:history @saved)) :comment)))))

      (testing "truncates a comment too long for the row"
        (with-redefs [api/stack-compose (constantly "compose-long-comment")]
          (api/append-history! "app" {:by      "bob"
                                      :comment (apply str (repeat 300 "x"))
                                      :trigger {:kind "stack-update"}})
          (let [c (:comment (last (:history @saved)))]
            (is (= 120 (count c)))
            (is (clojure.string/ends-with? c "…")))))

      (testing "skips when the compose is unchanged"
        (reset! saved nil)
        (with-redefs [api/stack-compose (constantly "compose-long-comment")]
          (api/append-history! "app" {:by "bob" :trigger {:kind "stack-redeploy"}})
          (is (nil? @saved))))

      (testing "keeps every entry — no arbitrary cap"
        (reset! stackfile {:name "app" :history []})
        (doseq [i (range 30)]
          (with-redefs [api/stack-compose (constantly (str "compose-" i))]
            (api/append-history! "app" {:by "bob" :trigger {:kind "stack-update"}})))
        (is (= 30 (count (:history @stackfile)))))

      (testing "a failing write is logged, not propagated"
        (with-redefs [api/stack-compose (fn [_] (throw (ex-info "boom" {})))]
          (is (nil? (api/append-history! "app" {:by "bob" :trigger {:kind "stack-update"}})))))))

  (testing "skips silently when the stack has no stackfile"
    (with-redefs [cc/stackfile (constantly nil)
                  cc/update-stackfile (fn [& _] (throw (AssertionError. "must not write")))]
      (is (nil? (api/append-history! "app" {:by "bob" :trigger {:kind "stack-update"}})))))

  (testing "skips when there is no stack name"
    (with-redefs [cc/stackfile (fn [& _] (throw (AssertionError. "must not read")))]
      (is (nil? (api/append-history! nil {:by "bob" :trigger {:kind "stack-update"}}))))))
