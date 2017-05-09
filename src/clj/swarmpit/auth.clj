(ns swarmpit.auth
  (:require [clj-jwt.core :refer :all]
            [clj-jwt.key :refer [private-key public-key]]
            [clj-time.core :refer [now plus days]]))

(def rsa-prv-key (private-key "rsa/private.key" "pass phrase"))
(def rsa-pub-key (public-key "rsa/public.key"))

(def claim
  {:iss "foo"
   :exp (plus (now) (days 1))
   :iat (now)})

(defn generate-token
  [claim]
  (-> claim jwt (sign :RS256 rsa-prv-key) to-str))

(defn verify-token
  [token]
  (-> token str->jwt (verify rsa-pub-key)))
