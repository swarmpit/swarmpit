(ns swarmpit.authentication
  (:import (clojure.lang ExceptionInfo))
  (:require [buddy.auth.backends.token :refer [jws-backend]]
            [buddy.auth.middleware :refer [authentication-request]]
            [swarmpit.handler :refer [resp-unauthorized]]
            [swarmpit.couchdb.client :as cc]))

(defn- authfn
  [{:keys [iss jti usr] :as identity}]
  (if (= "swarmpit-api" iss)
    (when-not (= jti (-> (:username usr)
                         (cc/user-by-username)
                         :api-token :jti))
      (throw (ex-info "API token invalid" {:cause :other}))))
  identity)

(defn- authentication-backend
  [secret]
  (jws-backend
    {:secret     secret
     :authfn     authfn
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
                        :other ex
                        (Throwable->map ex))]
            (-> (resp-unauthorized error)
                (assoc :headers {"X-Backend-Server" "swarmpit"}))))))))
