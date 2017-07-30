(ns swarmpit.server
  (:gen-class)
  (:import (clojure.lang ExceptionInfo))
  (:require [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [swarmpit.authentication :refer [wrap-authentication]]
            [swarmpit.authorization :refer [wrap-authorization]]
            [swarmpit.handler :as handler :refer :all]
            [org.httpkit.server :refer [run-server]]
            [cheshire.core :refer [parse-string]]
            [bidi.ring :refer [make-handler]]
            [bidi.bidi :refer [match-pair match-route*]]
            [clojure.tools.logging :as log]
            [swarmpit.routes :as routes]
            [swarmpit.install :as install]
            [swarmpit.agent :as agent]))

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

(defn wrap-path-params
  [handler]
  (fn [{:keys [uri] :as request}]
    (let [{:keys [route-params]} (match-route* routes/backend uri request)]
      (handler
        (assoc request :path-params route-params)))))

(def app
  (-> (make-handler routes/backend handler/dispatch)
      (wrap-resource "public")
      (wrap-resource "react")
      wrap-authorization
      wrap-client-exception
      wrap-fallback-exception
      wrap-authentication
      wrap-path-params
      wrap-json-params
      wrap-json-response
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      wrap-gzip))

(defn -main [& [port]]
  (install/init)
  (let [port (or port 8080)]
    (run-server app {:port port})
    (log/info "Server running on port" port))
  (agent/init))
