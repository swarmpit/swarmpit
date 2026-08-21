(ns swarmpit.api-test
  (:require [clojure.test :refer :all]
            [digest :refer [digest]]
            [swarmpit.api :as api :refer :all]
            [swarmpit.config :as cfg]
            [swarmpit.docker.engine.client :as dc]
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

(def ^:private stack-timestamp #'api/stack-timestamp)

(deftest stack-timestamp-test
  (let [services [{:createdAt "2026-01-02T10:00:00Z" :updatedAt "2026-03-01T10:00:00Z"}
                  {:createdAt "2026-01-01T10:00:00Z" :updatedAt "2026-02-01T10:00:00Z"}]]

    (testing "oldest creation and newest update across the stack services (#705)"
      (is (= "2026-01-01T10:00:00Z" (stack-timestamp services :createdAt first)))
      (is (= "2026-03-01T10:00:00Z" (stack-timestamp services :updatedAt last))))

    (testing "services missing a timestamp are skipped, not compared as nil"
      (is (= "2026-01-01T10:00:00Z"
             (stack-timestamp (conj services {:serviceName "no-times"}) :createdAt first))))

    (testing "nil when nothing carries a timestamp"
      (is (nil? (stack-timestamp [{:serviceName "a"}] :createdAt first)))
      (is (nil? (stack-timestamp [] :updatedAt last))))))

(defn- redeployed-image
  "Image that redeploy-service would write, given the digest the registry
   resolves and the digest currently pinned on the service."
  [{:keys [current-digest resolved-digest new-tag tag]
    :or   {tag "1.2"}}]
  (let [image (atom nil)
        spec {:Name         "app"
              :Mode         {:Replicated {:Replicas 1}}
              :TaskTemplate {:ForceUpdate   3
                             :ContainerSpec {:Image (str "nginx:" tag
                                                         (when current-digest (str "@" current-digest)))}}}]
    (with-redefs [dc/service          (constantly {:Spec spec :Version {:Index 7}})
                  dc/update-service   (fn [_ _ _ s] (reset! image (get-in s [:TaskTemplate :ContainerSpec :Image])))
                  api/repository-digest (constantly resolved-digest)]
      (api/redeploy-service nil "svc1" new-tag nil))
    @image))

(deftest redeploy-service-digest-test
  (testing "a resolved digest pins the image"
    (is (= "nginx:1.2@sha256:bbb"
           (redeployed-image {:current-digest "sha256:aaa" :resolved-digest "sha256:bbb"}))))

  (testing "a failed registry lookup keeps the existing pin instead of un-pinning (#738)"
    (is (= "nginx:1.2@sha256:aaa"
           (redeployed-image {:current-digest "sha256:aaa" :resolved-digest nil})))
    (is (= "nginx:1.2@sha256:aaa"
           (redeployed-image {:current-digest "sha256:aaa" :resolved-digest ""}))))

  (testing "a failed lookup while retagging must not carry the old tag's digest over"
    (is (= "nginx:2.0"
           (redeployed-image {:current-digest "sha256:aaa" :resolved-digest nil :new-tag "2.0"}))))

  (testing "unpinned service with no resolvable digest stays unpinned"
    (is (= "nginx:1.2"
           (redeployed-image {:current-digest nil :resolved-digest nil})))))
