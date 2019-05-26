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

(defn ->acr
  [acr]
  (dissoc acr :spPassword))

(defn ->acrs
  [acrs]
  (->> acrs
       (map ->acr)
       (into [])))

(defn ->gitlab-registry
  [registry]
  (dissoc registry :token))

(defn ->gitlab-registries
  [registries]
  (->> registries
       (map ->gitlab-registry)
       (into [])))

(defn ->user
  [user]
  (dissoc user :password))

(defn ->users
  [users]
  (->> users
       (map ->user)
       (into [])))

(defn ->dockerhub
  [dockeruser]
  (dissoc dockeruser :password))

(defn ->dockerhubs
  [dockerusers]
  (->> dockerusers
       (map ->dockerhub)
       (into [])))