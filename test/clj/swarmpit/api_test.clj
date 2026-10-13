(ns swarmpit.api-test
  (:require [clojure.test :refer :all]
            [digest :refer [digest]]
            [swarmpit.api :as api :refer :all]
            [swarmpit.config :as cfg]
            [swarmpit.docker.engine.client :as dc]
            [swarmpit.docker.engine.cli :as dcli]
            [swarmpit.http :as http]
            [swarmpit.couchdb.client :as cc]
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
           (redeployed-image {:current-digest nil :resolved-digest nil}))))

  (testing "an absent ForceUpdate counter starts at 1 instead of NPEing"
    (let [forced (atom nil)]
      (with-redefs [dc/service (constantly {:Spec    {:Name "app"
                                                      :Mode {:Replicated {:Replicas 1}}
                                                      :TaskTemplate {:ContainerSpec {:Image "nginx:1.2"}}}
                                            :Version {:Index 7}})
                    dc/update-service (fn [_ _ _ s] (reset! forced (get-in s [:TaskTemplate :ForceUpdate])))
                    api/repository-digest (constantly "sha256:bbb")]
        (api/redeploy-service nil "svc1" nil nil))
      (is (= 1 @forced)))))

(defn- fake-ghcr
  "ghcr.io token flow: 401 challenge, anonymous pull token, digest with that token."
  [requests public?]
  (fn [{:keys [url options] :as request}]
    (swap! requests conj request)
    (cond
      (and public? (= "https://ghcr.io/token" url))
      {:status 200 :body {:token "anon"}}

      (and public? (= "Bearer anon" (get-in options [:headers :Authorization])))
      {:status  200
       :headers {:content-type          "application/vnd.oci.image.index.v1+json"
                 :docker-content-digest "sha256:bbb"}}

      :else
      (throw (ex-info "Registry error: unauthorized"
                      {:status  401
                       :type    :http-client
                       :headers {:www-authenticate "Bearer realm=\"https://ghcr.io/token\",service=\"ghcr.io\",scope=\"repository:acme/app:pull\""}
                       :body    {:error "unauthorized"}})))))

(defn- redeploy-unlinked
  [public?]
  (let [requests (atom [])
        update (atom nil)]
    (with-redefs [api/supported-registries (constantly [])
                  http/execute-in-scope (fake-ghcr requests public?)
                  dc/service (constantly {:Spec    {:Name         "app"
                                                    :Mode         {:Replicated {:Replicas 1}}
                                                    :TaskTemplate {:ContainerSpec {:Image "ghcr.io/acme/app:latest@sha256:aaa"}}}
                                          :Version {:Index 7}})
                  dc/update-service (fn [auth _ _ s]
                                      (reset! update {:auth  auth
                                                      :image (get-in s [:TaskTemplate :ContainerSpec :Image])}))]
      (api/redeploy-service nil "svc1" nil))
    (assoc @update :requests @requests)))

(deftest unlinked-registry-test
  (testing "a public image on an unlinked registry resolves its digest anonymously"
    (let [{:keys [auth image requests]} (redeploy-unlinked true)
          token-request (first (filter #(= "https://ghcr.io/token" (:url %)) requests))]
      (is (= "ghcr.io/acme/app:latest@sha256:bbb" image))
      (is (nil? auth))
      (is (= "repository:acme/app:pull" (get-in token-request [:options :query-params :scope])))
      (is (nil? (get-in token-request [:options :headers :Authorization])))))

  (testing "an unresolvable image on an unlinked registry keeps its pin instead of failing with 401"
    (let [{:keys [auth image]} (redeploy-unlinked false)]
      (is (= "ghcr.io/acme/app:latest@sha256:aaa" image))
      (is (nil? auth))))

  (testing "a linked registry still hands its credentials to docker"
    (with-redefs [api/supported-registries (constantly [{:type     "v2"
                                                         :url      "https://ghcr.io"
                                                         :username "bot"
                                                         :password "pat"}])]
      (is (= {:username "bot" :password "pat" :serveraddress "https://ghcr.io"}
             (#'api/service-auth nil {:repository {:name "ghcr.io/acme/app"}}))))))

(deftest delete-stackfile-test
  (testing "a missing doc is a no-op rather than DELETE /swarmpit/"
    (let [deleted (atom [])]
      (with-redefs [cc/stackfile (constantly nil)
                    cc/delete-stackfile #(swap! deleted conj %)]
        (is (nil? (api/delete-stackfile "external-stack"))))
      (is (empty? @deleted))))

  (testing "an existing doc is deleted"
    (let [doc {:_id "abc" :_rev "1-x" :name "web"}
          deleted (atom [])]
      (with-redefs [cc/stackfile (constantly doc)
                    cc/delete-stackfile #(swap! deleted conj %)]
        (api/delete-stackfile "web"))
      (is (= [doc] @deleted)))))

(deftest deactivate-stack-test
  (testing "a stack swarmpit deployed keeps its stored stackfile untouched (#741)"
    (let [created (atom [])]
      (with-redefs [cc/stackfile (constantly {:name "web" :spec {:compose "existing"}})
                    api/stack-compose (fn [_] (throw (ex-info "must not be called" {})))
                    api/create-stackfile #(swap! created conj %)
                    dcli/stack-remove (constantly {:result "removed"})]
        (is (= {:result "removed"} (api/deactivate-stack "web"))))
      (is (empty? @created))))

  (testing "a stack without a stackfile is snapshotted from live state first (#741)"
    (let [events (atom [])]
      (with-redefs [cc/stackfile (constantly nil)
                    api/stack-compose (fn [_] "services:\n  web: {}\n")
                    api/create-stackfile #(swap! events conj [:created %])
                    dcli/stack-remove (fn [n] (swap! events conj [:removed n]) {:result "removed"})]
        (api/deactivate-stack "web"))
      (is (= [[:created {:name "web" :spec {:compose "services:\n  web: {}\n"}}]
              [:removed "web"]]
             @events)
          "the snapshot must happen before the stack is removed")))

  (testing "an unrenderable stack is still removed"
    (let [removed (atom nil)]
      (with-redefs [cc/stackfile (constantly nil)
                    api/stack-compose (constantly nil)
                    api/create-stackfile (fn [_] (throw (ex-info "must not be called" {})))
                    dcli/stack-remove (fn [n] (reset! removed n) {:result "removed"})]
        (api/deactivate-stack "web"))
      (is (= "web" @removed)))))

(deftest deployed-stacks-timestamps-test
  (let [svc (fn [stack name created updated]
              {:stack stack :serviceName name :createdAt created :updatedAt updated
               :networks [] :mounts [] :configs [] :secrets []})]

    (testing "stack times span the oldest create and newest update (#705)"
      (with-redefs [api/services (constantly [(svc "web" "a" "2026-01-02T10:00:00Z" "2026-03-01T10:00:00Z")
                                              (svc "web" "b" "2026-01-01T10:00:00Z" "2026-02-01T10:00:00Z")])]
        (let [{:keys [createdAt updatedAt]} (first (api/deployed-stacks))]
          (is (= "2026-01-01T10:00:00Z" createdAt))
          (is (= "2026-03-01T10:00:00Z" updatedAt)))))

    (testing "stacks are not conflated with each other"
      (with-redefs [api/services (constantly [(svc "web" "a" "2026-01-01T10:00:00Z" "2026-01-01T10:00:00Z")
                                              (svc "api" "b" "2026-05-01T10:00:00Z" "2026-05-01T10:00:00Z")])]
        (let [by-name (into {} (map (juxt :stackName :createdAt) (api/deployed-stacks)))]
          (is (= {"web" "2026-01-01T10:00:00Z"
                  "api" "2026-05-01T10:00:00Z"} by-name)))))

    (testing "services without a stack label are not a stack"
      (with-redefs [api/services (constantly [(svc nil "loose" "2026-01-01T10:00:00Z" "2026-01-01T10:00:00Z")])]
        (is (empty? (api/deployed-stacks)))))))
