(ns swarmpit.server
  (:import (clojure.lang ExceptionInfo))
  (:use [org.httpkit.server :only [run-server]])
  (:require [ring.middleware.json :as ring-json]
            [cheshire.core :refer [parse-string]]
            [swarmpit.handler :refer [handler json-error]]
            [swarmpit.token :as token]
            [swarmpit.agent :as agent]))

(def unsecure-api #{{:request-method :post
                     :uri            "/login"}})

(def admin-api #{{:request-method :get
                  :uri            "/users"}})

(defn- secure-api?
  [request]
  (let [api (select-keys request [:request-method :uri])]
    (not (contains? unsecure-api api))))

(defn- admin-api?
  [request]
  (let [api (select-keys request [:request-method :uri])]
    (contains? admin-api api)))

(defn- admin-access?
  [claims]
  (= "admin" (get-in claims [:usr :role])))

(defn wrap-client-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch ExceptionInfo e
        (ex-data e)))))

(defn wrap-fallback-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        {:status 500
         :body   (Throwable->map e)}))))

(defn wrap-auth-exception
  [handler]
  (fn [request]
    (if (secure-api? request)
      (let [headers (:headers request)
            token (get headers "authorization")]
        (if (some? token)
          (let [claims (try
                         (token/verify-jwt token)
                         (catch ExceptionInfo _
                           (json-error 401 "Invalid token")))]
            (if (admin-api? request)
              (if (admin-access? claims)
                (handler request)
                (json-error 401 "Unauthorized access"))
              (handler request)))
          (json-error 400 "Missing token")))
      (handler request))))

(def app
  (-> handler
      wrap-auth-exception
      wrap-client-exception
      ring-json/wrap-json-response
      ring-json/wrap-json-params
      wrap-fallback-exception))

(defn on-startup
  []
  (agent/start-repository-agent))


(defn -main [& _]
  (run-server app {:port 8080}))
