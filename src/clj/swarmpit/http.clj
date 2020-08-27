(ns swarmpit.http
  (:require [cheshire.core :refer [parse-string parse-stream generate-string]]
            [clj-http.client :as http]
            [taoensso.encore :as enc]
            [taoensso.timbre :refer [error]]
            [swarmpit.log :refer [pretty-print pretty-print-ex]])
  (:import (java.util.concurrent TimeoutException ExecutionException)
           (java.io IOException)
           (clojure.lang ExceptionInfo)))

(def default-timeout 15000)

(def ^:private req-func
  {:HEAD   http/head
   :GET    http/get
   :POST   http/post
   :PUT    http/put
   :DELETE http/delete})

(defn- req-options
  [options]
  (merge options
         (when (some? (:body options))
           {:body (generate-string (:body options))})))

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

(defn- log-error [{:keys [method url options scope] :as request} ex]
  (let [request-headers (:headers options)
        request-body (:body options)]
    (error
      (str
        "Request execution failed! Scope: " scope
        enc/system-newline "|> " (name method) " " url
        enc/system-newline "|> Headers: " (pretty-print request-headers)
        enc/system-newline "|> Payload: " (pretty-print request-body)
        enc/system-newline "|< Message: " (.getMessage ex)
        enc/system-newline "|< Data: " (pretty-print-ex (ex-data ex))))))

(defn execute-in-scope
  "Execute http request and parse result"
  [{:keys [method url options scope timeout error-handler] :as request}]
  (let [scope (or scope "HTTP")
        timeout (or timeout default-timeout)
        request-method (req-func method)
        request-options (req-options options)]
    (try
      (let [response (with-timeout timeout (request-method url request-options))
            response-body (-> response :body)
            response-headers (-> response :headers)
            response-status (-> response :status)
            response-body (ok-response response-body)]
        {:status  response-status
         :headers response-headers
         :body    response-body})
      (catch IOException exception
        (log-error request exception)
        (throw
          (let [error (.getMessage exception)]
            (ex-info (str scope " failure: " error)
                     {:status 500
                      :type   :http-client
                      :body   {:error error}}))))
      (catch ExceptionInfo exception
        (log-error request exception)
        (throw
          (let [data (some-> exception (ex-data))
                status (:status data)
                headers (:headers data)
                error (error-response data error-handler)]
            (ex-info (str scope " error: " error)
                     {:status  status
                      :type    :http-client
                      :headers headers
                      :body    {:error error}}))))
      (catch TimeoutException exception
        (log-error request exception)
        (throw
          (ex-info (str scope " error: Request timeout")
                   {:status 408
                    :type   :http-client
                    :body   {:error "Request timeout"}}))))))