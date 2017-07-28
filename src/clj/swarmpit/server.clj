(ns swarmpit.server
  (:gen-class)
  (:import (clojure.lang ExceptionInfo))
  (:require [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [buddy.auth.backends.token :refer [jws-backend]]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.accessrules :refer [success error wrap-access-rules]]
            [buddy.auth.middleware :refer [authentication-request]]
            [org.httpkit.server :refer [run-server]]
            [cheshire.core :refer [parse-string]]
            [bidi.ring :refer [make-handler]]
            [clojure.tools.logging :as log]
            [clojure.string :refer [starts-with?]]
            [swarmpit.handler :as handler :refer :all]
            [swarmpit.routes :as routes]
            [swarmpit.install :as install]
            [swarmpit.agent :as agent]
            [swarmpit.couchdb.client :as cc]))

(defn authentication-backend
  [secret]
  (jws-backend
    {:secret     secret
     :token-name "Bearer"
     :on-error   (fn [_ ex] (throw ex))}))

(defn authenticated-access
  [request]
  (if (authenticated? request)
    true
    (error {:code    401
            :message "Authentication failed"})))

(defn any-access
  [_]
  true)

(defn admin-access
  [request]
  (let [role (get-in (:identity request) [:usr :role])]
    (if (= "admin" role)
      true
      (error {:code    403
              :message "Unauthorized access"}))))

(def rules [{:pattern #"^/admin/.*"
             :handler {:and [authenticated-access admin-access]}}
            {:pattern #"^/login$"
             :handler any-access}
            {:pattern #"^/$"
             :handler any-access}
            {:pattern #"^/.*"
             :handler authenticated-access}])

(defn rules-error
  [_ val]
  (resp-error (:code val)
              (:message val)))

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

(defn wrap-authentication
  [handler]
  (fn [request]
    (let [secret (:secret (cc/get-secret))
          auth-backend (authentication-backend secret)]
      (try
        (handler (authentication-request request auth-backend))
        (catch ExceptionInfo ex
          (let [error (case (:cause (ex-data ex))
                        :exp "Token expired"
                        :signature "Token corrupted"
                        "Token invalid")]
            (resp-unauthorized error)))))))

(def app
  (-> (make-handler routes/backend handler/dispatch)
      (wrap-resource "public")
      (wrap-resource "react")
      (wrap-access-rules {:rules    rules
                          :on-error rules-error})
      wrap-authentication
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
    (log/info "Server running on port" port))
  (agent/init))
