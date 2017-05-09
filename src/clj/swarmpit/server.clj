(ns swarmpit.server
  (:import (clojure.lang ExceptionInfo))
  (:use [org.httpkit.server :only [run-server]])
  (:require [ring.middleware.json :as ring-json]
            [cheshire.core :refer [parse-string]]
            [swarmpit.handler :refer [handler json-error]]
            [swarmpit.utils :refer [in?]]))

(def unsecure ["/login"])

(defn- secure?
  [request]
  (let [uri (:uri request)]
    (not (in? unsecure uri))))

(defn wrap-client-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch ExceptionInfo e
        (let [response (ex-data e)]
          (json-error (:code response)
                      (:message response)))))))

(defn wrap-fallback-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        {:status 500 :body e}))))

(defn wrap-authentication
  [handler]
  (fn [request]
    (if (secure? request)
      (let [headers (:headers request)
            token (get headers "authorization")]
        (if (some? token)
          (if true
            (handler request)
            (json-error 401 "Invalid token"))
          (json-error 400 "Missing token")))
      (handler request))))

(def app
  (-> handler
      wrap-authentication
      wrap-client-exception
      ring-json/wrap-json-response
      ring-json/wrap-json-params
      wrap-fallback-exception))

(defn -main [& _]
  (run-server app {:port 8080}))
