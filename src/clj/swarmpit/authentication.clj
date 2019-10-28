(ns swarmpit.authentication
  (:import (clojure.lang ExceptionInfo))
  (:require [buddy.auth.backends.token :refer [jws-backend]]
            [buddy.auth.middleware :refer [authentication-request]]
            [swarmpit.handler :refer [resp-unauthorized]]
            [swarmpit.couchdb.client :as cc]))

(defn- authfn
  [{:keys [iss jti usr] :as identity}]
  (let [user (cc/user-by-username (:username usr))]
    (if user
      (if (= "swarmpit-api" iss)
        (when-not (= jti (-> user :api-token :jti))
          (throw (ex-info "Token discarded" {:cause :discarded}))))
      (throw (ex-info "Token rejected" {:cause :rejected})))
    identity))

(defn- authentication-backend
  [secret]
  (jws-backend
    {:secret     secret
     :authfn     authfn
     :token-name "Bearer"
     :on-error   (fn [_ ex] (throw ex))}))

(defn authentication-middleware
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
                        :discarded "Token discarded" ;; User API token removed
                        :rejected "Token rejected" ;; User does not exist
                        (Throwable->map ex))]
            (-> (resp-unauthorized error)
                (assoc :headers {"X-Backend-Server" "swarmpit"}))))))))
