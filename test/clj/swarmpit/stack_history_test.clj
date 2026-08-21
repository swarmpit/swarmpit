(ns swarmpit.stack-history-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
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

      (testing "skips when the compose is unchanged"
        (reset! saved nil)
        (with-redefs [api/stack-compose (constantly "version: '3'")]
          (api/append-history! "app" {:by "bob" :trigger {:kind "stack-redeploy"}})
          (is (nil? @saved))))

      (testing "keeps every entry — no arbitrary cap"
        (doseq [i (range 30)]
          (with-redefs [api/stack-compose (constantly (str "compose-" i))]
            (api/append-history! "app" {:by "bob" :trigger {:kind "stack-update"}})))
        (is (= 31 (count (:history @stackfile)))))

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
