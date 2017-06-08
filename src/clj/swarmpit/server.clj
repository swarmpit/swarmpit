(ns swarmpit.server
  (:gen-class)
  (:import (clojure.lang ExceptionInfo))
  (:require [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.json :as ring-json]
            [org.httpkit.server :refer [run-server]]
            [cheshire.core :refer [parse-string]]
            [bidi.ring :refer [make-handler]]
            [clojure.string :refer [starts-with?]]
            [swarmpit.handler :as handler :refer :all]
            [swarmpit.token :as token]))

(def routes
  ["/" {"login"          {:post login}
        "registries/"    {:get {"sum" registries-sum}}
        "services"       {:get  services
                          :post service-create}
        "services/"      {:get    {[:id] service}
                          :delete {[:id] service-delete}
                          :post   {[:id] service-update}}
        "networks"       {:get  networks
                          :post network-create}
        "networks/"      {:get    {[:id] network}
                          :delete {[:id] network-delete}}
        "nodes"          {:get nodes}
        "nodes/"         {:get {[:id] node}}
        "tasks"          {:get tasks}
        "tasks/"         {:get {[:id] task}}
        "v1/registries/" {:get {[:registryName "/repo"] {""      v1-repositories
                                                         "/tags" v1-repository-tags}}}
        "v2/registries/" {:get {[:registryName "/repo"] {""      v2-repositories
                                                         "/tags" v2-repository-tags}}}
        "admin/"         {"users"       {:get  users
                                         :post user-create}
                          "users/"      {:get {[:id] handler/user}}
                          "registries"  {:get  registries
                                         :post registry-create}
                          "registries/" {:get {[:id] registry}}}}])

(def unsecure-api #{{:request-method :post
                     :uri            "/login"}
                    {:request-method :get
                     :uri            "/index.html"}
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
  (-> (make-handler routes)
      (wrap-resource "public")
      (wrap-resource "react")
      wrap-auth-exception
      wrap-client-exception
      ring-json/wrap-json-response
      ring-json/wrap-json-params
      wrap-fallback-exception
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      wrap-gzip))

(defn -main [& [port]]
  (let [port (or port 8080)]
    (run-server app {:port port})
    (println (str "Server running on port " port))))
