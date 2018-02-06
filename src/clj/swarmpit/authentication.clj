(ns swarmpit.authentication
  (:import (clojure.lang ExceptionInfo))
  (:require [buddy.auth.backends.token :refer [jws-backend]]
            [buddy.auth.middleware :refer [authentication-request]]
            [swarmpit.handler :refer [resp-unauthorized]]
            [swarmpit.couchdb.client :as cc]))

(defn- authentication-backend
  [secret]
  (jws-backend
    {:secret     secret
     :token-name "Bearer"
     :on-error   (fn [_ ex] (throw ex))}))

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
                        :signature "Token invalid"
                        (Throwable->map ex))]
            (-> (resp-unauthorized error)
                (assoc :headers {"X-Backend-Server" "swarmpit"}))))))))
