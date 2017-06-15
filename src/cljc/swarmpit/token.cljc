(ns swarmpit.token
  #?(:clj
     (:import java.util.Base64))
  (:require
    #?@(:clj  [[clojure.string :as str]
               [swarmpit.uuid :refer [uuid]]
               [buddy.sign.jwt :as jwt]
               [clj-time.core :refer [now plus days]]]
        :cljs [[clojure.string :as str]
               [cognitect.transit :as t]
               [clojure.walk :refer [keywordize-keys]]
               [goog.crypt.base64 :as b64]])))

(def r (t/reader :json))

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
   (defn generate-base64
     [credentials]
     (let [credentials-bytes (.getBytes credentials)]
       (.encodeToString (Base64/getEncoder) credentials-bytes))))

#?(:clj
   (defn generate-basic
     [username password]
     (let [credentials (credentials username password)
           base64 (generate-base64 credentials)]
       (basic base64))))

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

#?(:clj
   (defn decode-basic
     [token]
     (let [decoded (String. (.decode (Base64/getDecoder)
                                     (token-value token)))
           credentials (str/split decoded #":")]
       {:username (first credentials)
        :password (second credentials)})))