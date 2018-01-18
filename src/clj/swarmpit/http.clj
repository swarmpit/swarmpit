(ns swarmpit.http
  (:require [cheshire.core :refer [parse-string parse-stream generate-string]]
            [clj-http.client :as http])
  (:import (java.util.concurrent TimeoutException ExecutionException)
           (java.io IOException)
           (clojure.lang ExceptionInfo)))

(def default-timeout 15000)

(def ^:private req-func
  {:GET    http/get
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

(defn execute-in-scope
  "Execute http request and parse result"
  [{:keys [method url options scope timeout error-handler]}]
  (let [scope (or scope "HTTP")
        timeout (or timeout default-timeout)]
    (try
      (let [response (with-timeout timeout ((req-func method) url (req-options options)))
            response-body (-> response :body)
            response-headers (-> response :headers)]
        {:headers response-headers
         :body    (ok-response response-body)})
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
                headers (:headers data)
                error (error-response data error-handler)]
            (ex-info (str scope " error: " error)
                     {:status  status
                      :headers headers
                      :body    {:error error}}))))
      (catch TimeoutException _
        (throw
          (ex-info (str scope " error: Request timeout")
                   {:status 408
                    :body   {:error "Request timeout"}}))))))