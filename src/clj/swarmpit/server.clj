(ns swarmpit.server
  (:gen-class)
  (:import (clojure.lang ExceptionInfo))
  (:require [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [swarmpit.authentication :refer [authentication-middleware]]
            [swarmpit.authorization :refer [authorization-middleware]]
            [swarmpit.event.handler :refer :all]
            [org.httpkit.server :refer [run-server]]
            [clojure.tools.logging :as log]
            [swarmpit.routes :as routes]
            [swarmpit.setup :as setup]
            [swarmpit.database :as db]
            [swarmpit.agent :as agent]
            [reitit.ring :as ring]
            [reitit.coercion.spec]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.ring.coercion :as coercion]
            [reitit.dev.pretty :as pretty]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [muuntaja.core :as m]))

(defn client-exception-middleware
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch ExceptionInfo e
        (log/info (ex-data e))
        (dissoc (ex-data e) :headers)))))

(defn fallback-exception-middleware
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        {:status 500
         :body   (Throwable->map e)}))))

(def app
  (ring/ring-handler
    (ring/router
      routes/backend
      {:exception pretty/exception
       :conflicts nil
       :data      {:coercion   reitit.coercion.spec/coercion
                   :muuntaja   m/instance
                   :middleware [;; negotiation, request decoding and response encoding
                                muuntaja/format-middleware
                                ;; authentication
                                authentication-middleware
                                ;; authorization
                                authorization-middleware
                                ;; swagger feature
                                swagger/swagger-feature
                                ;; query-params & form-params
                                parameters/parameters-middleware
                                ;; exception handling
                                fallback-exception-middleware
                                client-exception-middleware
                                ;; coercing response bodys
                                coercion/coerce-response-middleware
                                ;; coercing request parameters
                                coercion/coerce-request-middleware]
                   :swagger    {:produces #{"application/json"
                                            "application/edn"
                                            "application/transit+json"}
                                :consumes #{"application/json"
                                            "application/edn"
                                            "application/transit+json"}}}})
    (ring/routes
      (swagger-ui/create-swagger-ui-handler
        {:path   "/docs"
         :url    "/api/swagger.json"
         :config {:validatorUrl     nil
                  :operationsSorter "alpha"}})
      (-> (ring/create-default-handler)
          (wrap-resource "public")
          (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
          wrap-gzip))))

(defn -main [& [port]]
  (log/info "Swarmpit is starting...")
  (db/init)
  (let [port (or port 8080)]
    (run-server app {:port port} :thread 8 :queue-size 300000)
    (log/info "Swarmpit running on port" port))
  (agent/init)
  (setup/docker))