(ns swarmpit.couchdb.mapper.outbound
  (:require [digest :as d]
            [swarmpit.uuid :refer [uuid]]))

(defn ->password
  [password]
  (d/digest "sha-256" password))

(defn ->user
  [user]
  (assoc user :password (->password (:password user))
              :type "user"
              :id (uuid)))

(defn ->registry
  [registry]
  (assoc registry :type "registry"
                  :id (uuid)))


