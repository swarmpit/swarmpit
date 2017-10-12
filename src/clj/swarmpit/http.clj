(ns swarmpit.http
  (:require [cheshire.core :refer [parse-string parse-stream generate-string]])
  (:import (java.util.concurrent TimeoutException ExecutionException)
           (java.io IOException)
           (clojure.lang ExceptionInfo)))

(def default-timeout 15000)

(defmacro with-timeout
  [ms & body]
  `(try
     (let [f# (future (do ~@body))
           v# (gensym)
           result# (deref f# ~ms v#)]
       (if (= v# result#)
         (do
           (future-cancel f#)
           (throw (TimeoutException.)))
         result#))
     (catch ExecutionException e#
       (throw (.getCause e#)))))

(defn- error-response
  [response-data error-handler]
  (let [error-handler (or error-handler :error)]
    (try
      (-> (:body response-data)
          (parse-string true)
          (error-handler))
      (catch Exception _
        (:reason-phrase response-data)))))

(defn- ok-response
  [response-data]
  (try
    (parse-string response-data true)
    (catch Exception _
      response-data)))

(defn execute-in-scope
  "Execute http request and parse result"
  [{:keys [call-fx scope timeout error-handler]}]
  (let [scope (or scope "HTTP")
        timeout (or timeout default-timeout)]
    (try
      (let [response (with-timeout timeout (call-fx))
            response-body (-> response :body)]
        (ok-response response-body))
      (catch IOException exception
        (throw
          (let [error (.getMessage exception)]
            (ex-info (str scope " failure: " error)
                     {:status 500
                      :body   {:error error}}))))
      (catch ExceptionInfo exception
        (throw
          (let [data (some-> exception (ex-data))
                status (:status data)
                error (error-response data error-handler)]
            (ex-info (str scope " error: " error)
                     {:status status
                      :body   {:error error}})))))))