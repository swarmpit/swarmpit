(ns swarmpit.token
  #?(:clj
     (:import java.util.Base64))
  (:require
    #?@(:clj  [[clojure.string :as str]
               [swarmpit.utils :refer [uuid]]
               [buddy.sign.jwt :as jwt]
               [clj-time.core :refer [now plus days]]]
        :cljs [[clojure.string :as str]
               [goog.crypt.base64 :as b64]])))

(defn- token-value
  [token]
  (second (str/split token #" ")))

(defn- credentials
  [username password]
  (str username ":" password))

(defn- bearer
  [token]
  (str "Bearer " token))

(defn- basic
  [token]
  (str "Basic " token))

#?(:clj
   (defn claim
     [user]
     {:iss "swarmpit"
      :exp (plus (now) (days 1))
      :iat (now)
      :usr (select-keys user [:username :email :role])
      :jti (uuid)}))

#?(:clj
   (defn generate-jwt
     [user]
     (let [jwt (jwt/sign (claim user) "secret")]
       (bearer jwt))))

#?(:clj
   (defn verify-jwt
     [token]
     (-> (token-value token)
         (jwt/unsign "secret"))))

#?(:clj
   (defn generate-basic
     [username password]
     (let [credentials (credentials username password)
           credentials-bytes (.getBytes credentials)
           credentials-encoded (.encodeToString (Base64/getEncoder) credentials-bytes)]
       (basic credentials-encoded))))

#?(:cljs
   (defn generate-basic
     [username password]
     (let [credentials (credentials username password)
           credentials-encoded (b64/encodeString credentials)]
       (basic credentials-encoded))))

#?(:clj
   (defn decode-basic
     [token]
     (let [decoded (String. (.decode (Base64/getDecoder)
                                     (token-value token)))
           credentials (str/split decoded #":")]
       {:username (first credentials)
        :password (second credentials)})))