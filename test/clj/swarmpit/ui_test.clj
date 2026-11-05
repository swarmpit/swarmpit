(ns swarmpit.ui-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [swarmpit.config :as config]
            [swarmpit.handler :as handler]
            [swarmpit.version :as version])
  (:import (java.io ByteArrayInputStream)))

(def react-html
  "<!doctype html>\n<html><head><title>Swarmpit · přehled</title></head><body><div id=\"root\"></div><script type=\"module\" src=\"/react/assets/app.js\"></script></body></html>\n")

(deftest classic-ui-is-the-default
  (with-redefs [config/environment {}
                version/info (constantly {:revision "ui-test"})]
    (is (= "classic" (config/config :ui)))
    (let [response (handler/index {})]
      (is (= 200 (:status response)))
      (is (str/starts-with? (get-in response [:headers "Content-Type"]) "text/html"))
      (is (str/includes? (:body response) "css/main.css?r=ui-test"))
      (is (str/includes? (:body response) "js/main.js?r=ui-test")))))

(deftest react-ui-serves-the-packaged-html-unchanged
  (let [resources (atom [])]
    (with-redefs [config/config (constantly "react")
                  io/resource (fn [path]
                                (swap! resources conj path)
                                (when (= path "public/react/index.html")
                                  (ByteArrayInputStream. (.getBytes react-html "UTF-8"))))
                  handler/index-page (fn [& _] (throw (AssertionError. "React must not render the classic template")))
                  version/info (fn [] (throw (AssertionError. "React must not request the classic revision")))]
      (let [response (handler/index {})]
        (is (= 200 (:status response)))
        (is (= react-html (:body response)))
        (is (str/includes? (get-in response [:headers "Content-Type"]) "charset=utf-8"))
        (is (= ["public/react/index.html"] @resources))))))

(deftest classic-query-overrides-a-react-default
  (with-redefs [config/config (constantly "react")
                version/info (constantly {:revision "rollback"})]
    (let [response (handler/index {:query-params {"ui" "classic"}})]
      (is (= 200 (:status response)))
      (is (str/includes? (:body response) "js/main.js?r=rollback"))
      (is (not (str/includes? (:body response) "/react/assets/"))))))

(deftest query-cannot-enable-an-unconfigured-react-ui
  (with-redefs [config/config (constantly "classic")
                version/info (constantly {:revision "classic"})]
    (is (str/includes? (:body (handler/index {:query-params {"ui" "react"}}))
                       "js/main.js?r=classic"))))

(deftest missing-react-assets-return-a-useful-service-unavailable-response
  (with-redefs [config/config (constantly "react")
                version/info (constantly {:revision "missing-react"})
                io/resource (constantly nil)]
    (let [response (handler/index {})]
      (is (= 503 (:status response)))
      (is (string? (:body response)))
      (is (str/includes? (:body response) "React UI"))
      (is (str/includes? (:body response) "public/react/index.html"))
      (is (str/includes? (:body response) "/?ui=classic")))))
