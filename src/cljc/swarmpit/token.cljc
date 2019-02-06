(ns swarmpit.token
  (:require
    #?@(:clj  [[clojure.string :as str]
               [swarmpit.uuid :refer [uuid]]
               [swarmpit.base64 :as base64]
               [swarmpit.couchdb.client :as cc]
               [buddy.sign.jwt :as jwt]
               [clj-time.core :refer [now plus days]]]
        :cljs [[clojure.string :as str]
               [cognitect.transit :as t]
               [clojure.walk :refer [keywordize-keys]]
               [goog.crypt.base64 :as b64]])))

#?(:cljs
   (def r (t/reader :json)))

(defn admin?
  [user]
  (= "admin" (:role user)))

(defn token-value
  [token]
  (second (str/split token #" ")))

(defn credentials
  [username password]
  (str username ":" password))

(defn bearer
  [token]
  (str "Bearer " token))

(defn basic
  [token]
  (str "Basic " token))

#?(:clj
   (defn claim
     ([user]
      (claim user nil))
     ([user options]
      (-> {:iss "swarmpit"
           :exp (plus (now) (days 1))
           :iat (now)
           :usr (select-keys user [:username :email :role])
           :jti (uuid)}
          (merge (select-keys options [:exp :jti :iss]))))))

#?(:clj
   (defn generate-jwt
     ([user]
      (generate-jwt user (claim user)))
     ([user options]
      (let [jwt (jwt/sign (claim user options) (:secret (cc/get-secret)))]
        (bearer jwt)))))

#?(:clj
   (defn verify-jwt
     [token]
     (-> (token-value token)
         (jwt/unsign (:secret (cc/get-secret))))))

#?(:clj
   (defn user
     [token]
     (get-in (verify-jwt token) [:usr :username])))

#?(:clj
   (defn generate-basic
     [username password]
     (let [credentials (credentials username password)
           base64 (base64/encode credentials)]
       (basic base64))))

#?(:clj
   (defn decode-basic
     [token]
     (let [decoded (base64/decode (token-value token))
           credentials (str/split decoded #":")]
       {:username (first credentials)
        :password (second credentials)})))

#?(:cljs
   (defn decode-jwt
     [token]
     (keywordize-keys
       (t/read r
               (-> (token-value token)
                   (clojure.string/split #"\.")
                   (second)
                   (b64/decodeString))))))

#?(:cljs
   (defn generate-basic
     [username password]
     (let [credentials (credentials username password)
           credentials-encoded (b64/encodeString credentials)]
       (basic credentials-encoded))))
