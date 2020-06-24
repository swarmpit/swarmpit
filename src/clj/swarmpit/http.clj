(ns swarmpit.http
  (:require [cheshire.core :refer [parse-string parse-stream generate-string]]
            [clj-http.client :as http]
            [taoensso.timbre :refer [info error]]
            [swarmpit.utils :refer [update-in-if-present]]
            [clojure.string :as str])
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

(defn- hide-sensitive-data [request-fragment-map]
  (update-in-if-present
    request-fragment-map
    [:password :secret :Authorization]
    (fn [a]
      (cond
        (str/starts-with? a "Basic") (str "Basic *****")
        (str/starts-with? a "Bearer") (str "Bearer *****")
        (str/starts-with? a "JWT") (str "JWT *****")
        :else "*****"))))

(defn- log-error [{:keys [method url options] :as request} ex]
  (let [request-headers (:headers options)
        request-body (:body options)]
    (info "|>" (name method) url)
    (doseq [[k v] (hide-sensitive-data request-headers)]
      (info "|>" (name k) ":" v))
    (when request-body
      (info "|>" (hide-sensitive-data request-body)))
    (error "|<" "Request Execution failed:" (.getMessage ex))
    (error "|<" ex)))

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
            response-body (ok-response response-body)]
        {:headers response-headers
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