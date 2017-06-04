(ns swarmpit.server
  (:import (clojure.lang ExceptionInfo))
  (:use [org.httpkit.server :only [run-server]])
  (:require [ring.middleware.json :as ring-json]
            [cheshire.core :refer [parse-string]]
            [bidi.ring :refer [make-handler]]
            [clojure.string :refer [starts-with?]]
            [swarmpit.handler :as handler :refer [json-error]]
            [swarmpit.token :as token]))

(def routes
  ["/" {"login"          {:post handler/login}
        "registries/"    {:get {"sum" handler/registries-sum}}
        "services"       {:get  handler/services
                          :post handler/service-create}
        "services/"      {:get    {[:id] handler/service}
                          :delete {[:id] handler/service-delete}
                          :post   {[:id] handler/service-update}}
        "networks"       {:get  handler/networks
                          :post handler/network-create}
        "networks/"      {:get    {[:id] handler/network}
                          :delete {[:id] handler/network-delete}}
        "nodes"          {:get handler/nodes}
        "nodes/"         {:get {[:id] handler/node}}
        "tasks"          {:get handler/tasks}
        "tasks/"         {:get {[:id] handler/task}}
        "v1/registries/" {:get {[:registryName "/repo"] {""      handler/v1-repositories
                                                         "/tags" handler/v1-repository-tags}}}
        "v2/registries/" {:get {[:registryName "/repo"] {""      handler/v2-repositories
                                                         "/tags" handler/v2-repository-tags}}}}
   "/admin/" {"users"      {:get handler/users}
              "registries" {:get  handler/registries
                            :post handler/registry-create}}])

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
                           (json-error 401 "Invalid token")))]
            (if (admin-api? request)
              (if (admin-access? claims)
                (handler request)
                (json-error 401 "Unauthorized access"))
              (handler request)))
          (json-error 400 "Missing token")))
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
