(ns swarmpit.server
  (:import (clojure.lang ExceptionInfo))
  (:use [org.httpkit.server :only [run-server]])
  (:require [ring.middleware.json :as ring-json]
            [cheshire.core :refer [parse-string]]
            [bidi.ring :refer [make-handler]]
            [clojure.string :refer [starts-with?]]
            [swarmpit.handler :refer :all]
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
        "admin/"         {:get  {"users"      {"" users}
                                 "registries" {"" registries}}
                          :post {"users"      {"" user-create}
                                 "registries" {"" registry-create}}}}])

(def unsecure-api #{{:request-method :post
                     :uri            "/login"}})

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
      wrap-auth-exception
      wrap-client-exception
      ring-json/wrap-json-response
      ring-json/wrap-json-params
      wrap-fallback-exception))

(defn -main [& _]
  (run-server app {:port 8080}))
