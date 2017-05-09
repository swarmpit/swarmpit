(ns swarmpit.token
  (:require [clj-jwt.core :refer :all]
            [clj-jwt.key :refer [private-key public-key]]
            [clj-time.core :refer [now plus days]]
            [swarmpit.utils :refer [generate-uuid]]))

(defn claim
  [user]
  {:iss "swarmpit"
   :exp (plus (now) (days 1))
   :iat (now)
   :usr user
   :jti (generate-uuid)})

(defn generate-token
  [claim]
  (-> claim jwt (sign :HS256 "secret") to-str))

(defn verify-token
  [token]
  (-> token str->jwt (verify "secret")))
