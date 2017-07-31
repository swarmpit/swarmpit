(ns swarmpit.response
  (:require
    #?@(:clj [[cheshire.core :refer [parse-string parse-stream]]]))
  (:import
    #?@(:clj [org.httpkit.BytesInputStream])))

#?(:clj
   (defn parse-response-body
     [body]
     (if (instance? BytesInputStream body)
       (parse-stream (clojure.java.io/reader body))
       (parse-string body true))))