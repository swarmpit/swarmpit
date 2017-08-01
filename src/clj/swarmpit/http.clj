(ns swarmpit.http
  (:require [cheshire.core :refer [parse-string parse-stream generate-string]])
  (:import (org.httpkit BytesInputStream)))

(defn parse-response-body
  [body]
  (if (instance? BytesInputStream body)
    (parse-stream (clojure.java.io/reader body))
    (parse-string body true)))

(defn execute-in-scope
  ([call-fx scope] (execute-in-scope call-fx scope :error))
  ([call-fx scope error-message-handler]
   (let [scope (or scope "HTTP")
         {:keys [status body error]} call-fx]
     (if error
       (throw
         (ex-info (str scope " failure: " (.getMessage error))
                  {:status 500
                   :body   {:error (.getMessage error)}}))
       (let [response (parse-response-body body)]
         (if (> 400 status)
           response
           (throw
             (ex-info (str scope " error: " (error-message-handler response))
                      {:status status
                       :body   response}))))))))