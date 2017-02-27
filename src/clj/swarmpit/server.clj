(ns swarmpit.server
  (:import (clojure.lang ExceptionInfo))
  (:use [org.httpkit.server :only [run-server]])
  (:require [swarmpit.handler :refer [handler]]
            [ring.middleware.json :as ring-json]
            [cheshire.core :refer [parse-string]]))

(defn wrap-client-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch ExceptionInfo e
        (let [response (ex-data e)]
          {:status  (:code response)
           :headers {"Content-Type" "application/json"}
           :body    {:error (:message response)}})))))

(defn wrap-fallback-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        {:status 500 :body e}))))

(def app
  (-> handler
      wrap-client-exception
      ring-json/wrap-json-response
      ring-json/wrap-json-params
      wrap-fallback-exception))

(defn -main [& _]
  (run-server app {:port 8080}))
