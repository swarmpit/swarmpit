(ns swarmpit.docker.registry.client-test
  (:require [clojure.test :refer :all]
            [swarmpit.docker.auth.client :as auth]
            [swarmpit.docker.registry.client :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(deftest ^:integration docker-registry-test

  (testing "error"
    (is (thrown-with-msg?
          ExceptionInfo #"Docker registry error: authentication required"
          (tags nil "nginx")))))

(defn- hub-digest
  [repository tag]
  (-> (auth/token nil repository) :token (digest repository tag)))

(deftest ^:integration oci-manifest-digest-test
  (testing "images published as an OCI index still resolve a digest (#738)"
    (doseq [repository ["library/nginx" "library/alpine" "library/traefik"]]
      (is (re-matches #"sha256:[0-9a-f]{64}" (str (hub-digest repository "latest")))
          (str repository " must resolve, else autoredeploy force-updates it every poll"))))

  (testing "images published as a docker manifest list keep working"
    (is (re-matches #"sha256:[0-9a-f]{64}" (str (hub-digest "grafana/grafana" "latest"))))))