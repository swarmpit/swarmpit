(ns swarmpit.docker.engine.cli-test
  (:require [clojure.test :refer :all]
            [swarmpit.docker.engine.cli :as cli]))

(defn- rejects? [f]
  (try (f) false
       (catch clojure.lang.ExceptionInfo _ true)))

(deftest validate-stack-name-accepts-sensible-names
  (doseq [name ["foo" "foo-bar" "foo_bar" "f" "F00" "a1b2"
                "my-stack-123" (apply str "a" (repeat 62 "b"))]]
    (is (= name
           (do (#'cli/validate-stack-name! name) name))
        (str "should accept " name))))

(deftest validate-stack-name-rejects-traversal
  (doseq [name ["../etc/passwd" "/etc/passwd" "foo/../bar" "foo/bar"
                "..\\evil" "foo\u0000bar"]]
    (is (rejects? #(#'cli/validate-stack-name! name))
        (str "should reject " (pr-str name)))))

(deftest validate-stack-name-rejects-shell-metachars
  (doseq [name ["foo;rm -rf /" "$(whoami)" "`id`" "foo|cat" "foo&bar"
                "foo bar" "foo\nbar" "foo>out" "foo'bar" "foo\"bar"]]
    (is (rejects? #(#'cli/validate-stack-name! name))
        (str "should reject " (pr-str name)))))

(deftest validate-stack-name-rejects-edge-cases
  (doseq [name [nil "" "-leading-dash" "_leading-underscore"
                (apply str (repeat 64 "a"))]]
    (is (rejects? #(#'cli/validate-stack-name! name))
        (str "should reject " (pr-str name)))))
