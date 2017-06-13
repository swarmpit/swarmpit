(ns swarmpit.couchdb.mapper.outbound
  (:require [digest :as d]
            [swarmpit.uuid :refer [uuid]]))

(defn ->password
  [password]
  (d/digest "sha-256" password))

(defn ->user
  [user]
  (-> (assoc user :password (->password (:password user))
                  :type "user")
      (dissoc :isValid)))

(defn ->registry
  [registry]
  (-> (assoc registry :type "registry")
      (dissoc :isValid)))

(defn ->docker-user
  [docker-user]
  (-> (assoc docker-user :type "dockeruser")
      (dissoc :isValid)))


