(ns swarmpit.server
  (:gen-class)
  (:import (clojure.lang ExceptionInfo))
  (:require [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [org.httpkit.server :refer [run-server]]
            [cheshire.core :refer [parse-string]]
            [bidi.ring :refer [make-handler]]
            [clojure.string :refer [starts-with?]]
            [swarmpit.handler :as handler :refer :all]
            [swarmpit.routes :as routes]
            [swarmpit.token :as token]
            [swarmpit.install :as install]))

(def unsecure-api #{{:request-method :post
                     :uri            "/login"}
                    {:request-method :get
                     :uri            "/"}
                    {:request-method :get
                     :uri            "/css/app.css"}
                    {:request-method :get
                     :uri            "/js/main.js"}
                    {:request-method :get
                     :uri            "/favicon.ico"}})

(defn- secure-api?
  [request]
  (let [api (select-keys request [:request-method :uri])]
    (not (contains? unsecure-api api))))

(defn- admin-api?
  [request]
  (starts-with? (:uri request) "/admin/"))

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
                           (resp-unauthorized "Invalid token")))]
            (if (admin-api? request)
              (if (admin-access? claims)
                (handler request)
                (resp-unauthorized "Unauthorized access"))
              (handler request)))
          (resp-error 400 "Missing token")))
      (handler request))))

(def app
  (-> (make-handler routes/backend handler/dispatch)
      (wrap-resource "public")
      (wrap-resource "react")
      wrap-auth-exception
      wrap-client-exception
      wrap-json-response
      wrap-json-params
      wrap-fallback-exception
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      wrap-gzip))

(defn -main [& [port]]
  (install/init)
  (let [port (or port 8080)]
    (run-server app {:port port})
    (println (str "Server running on port " port))))
