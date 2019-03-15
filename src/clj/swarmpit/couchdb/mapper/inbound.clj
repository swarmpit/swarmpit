(ns swarmpit.couchdb.mapper.inbound)

(defn ->registry
  [registry]
  (dissoc registry :password))

(defn ->registries
  [registries]
  (->> registries
       (map ->registry)
       (into [])))

(defn ->ecr
  [ecr]
  (dissoc ecr :accessKey))

(defn ->ecrs
  [ecrs]
  (->> ecrs
       (map ->ecr)
       (into [])))

(defn ->user
  [user]
  (dissoc user :password))

(defn ->users
  [users]
  (->> users
       (map ->user)
       (into [])))

(defn ->dockeruser
  [dockeruser]
  (dissoc dockeruser :password))

(defn ->dockerusers
  [dockerusers]
  (->> dockerusers
       (map ->dockeruser)
       (into [])))