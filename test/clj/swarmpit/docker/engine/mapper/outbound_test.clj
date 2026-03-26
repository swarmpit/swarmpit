(ns swarmpit.docker.engine.mapper.outbound-test
  (:require [clojure.test :refer :all]
            [swarmpit.docker.engine.mapper.outbound :refer [->service-image]]))

(deftest ->service-image-test
  (let [service {:repository {:name "nginx" :tag "alpine" :imageDigest "sha256:abc"}}]
    (testing "digest? true with digest uses tag+digest form"
      (is (= "nginx:alpine@sha256:abc" (->service-image service true))))

    (testing "digest? false uses tag form"
      (is (= "nginx:alpine" (->service-image service false)))))

  (testing "blank imageDigest falls back to tag form (issue #724)"
    (is (= "nginx:alpine"
           (->service-image {:repository {:name "nginx" :tag "alpine" :imageDigest ""}} true)))
    (is (= "nginx:alpine"
           (->service-image {:repository {:name "nginx" :tag "alpine" :imageDigest "   "}} true))))

  (testing "nil imageDigest falls back to tag form"
    (is (= "nginx:alpine"
           (->service-image {:repository {:name "nginx" :tag "alpine"}} true)))))
