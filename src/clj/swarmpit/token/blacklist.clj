(ns swarmpit.token.blacklist
  "In-memory TTL cache of revoked login-JWT JTIs. Entries auto-expire after
   the JWT's own max lifetime so the set stays bounded."
  (:require [clojure.core.cache :as cache]))

;; TTL longer than the login-JWT `:exp` so a revoked JTI stays blacklisted
;; until it would have expired anyway. 1 day token + 1 hour slack.
(def ^:private ttl-ms (* 25 60 60 1000))

(defonce ^:private store
  (atom (cache/ttl-cache-factory {} :ttl ttl-ms)))

(defn revoke!
  "Mark a JTI as revoked."
  [jti]
  (when jti
    (swap! store assoc jti true)))

(defn revoked?
  [jti]
  (and jti (cache/has? @store jti)))
