(ns swarmpit.authentication
  (:import (clojure.lang ExceptionInfo))
  (:require [buddy.auth.backends.token :refer [jws-backend]]
            [buddy.auth.middleware :refer [authentication-request]]
            [clojure.tools.logging :as log]
            [swarmpit.handler :refer [resp-error resp-unauthorized]]
            [swarmpit.couchdb.client :as cc]
            [swarmpit.token.blacklist :as blacklist]))

(defn- authfn
  [{:keys [iss jti usr] :as identity}]
  (let [user (cc/user-by-username (:username usr))]
    (if user
      (if (= "swarmpit-api" iss)
        (when-not (= jti (-> user :api-token :jti))
          (throw (ex-info "Token discarded" {:cause :discarded})))
        (when (blacklist/revoked? jti)
          (throw (ex-info "Token revoked" {:cause :revoked}))))
      (throw (ex-info "Token rejected" {:cause :rejected})))
    identity))

(defn- authentication-backend
  [secret]
  (jws-backend
    {:secret     secret
     :authfn     authfn
     :token-name "Bearer"
     :on-error   (fn [_ ex] (throw ex))}))

(defn- signing-secret
  []
  (try
    (cc/secret)
    (catch Exception ex
      (log/warn "Token secret unreachable:" (.getMessage ex))
      nil)))

(defn authentication-middleware
  [handler]
  (fn [request]
    (if-let [secret (signing-secret)]
      (let [auth-backend (authentication-backend secret)]
        (try
          (handler (authentication-request request auth-backend))
          (catch ExceptionInfo ex
            (let [error (case (:cause (ex-data ex))
                          :exp "Token expired"
                          :signature "Token invalid"
                          :discarded "Token discarded" ;; User API token removed
                          :revoked "Token revoked" ;; Login token revoked via logout
                          :rejected "Token rejected" ;; User does not exist
                          "Token invalid")]
              (-> (resp-unauthorized error)
                  (assoc :headers {"X-Backend-Server" "swarmpit"}))))))
      (-> (resp-error 503 "Database unavailable")
          (assoc :headers {"X-Backend-Server" "swarmpit"})))))
