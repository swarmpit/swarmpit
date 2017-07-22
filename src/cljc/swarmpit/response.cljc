(ns swarmpit.response
  (:require [cheshire.core :refer [parse-string parse-stream]])
  (:import (org.httpkit BytesInputStream)))

(defn- parse-response-body
  [body]
  (if (instance? BytesInputStream body)
    (parse-stream (clojure.java.io/reader body))
    (parse-string body true)))