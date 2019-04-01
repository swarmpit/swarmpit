(ns swarmpit.couchdb.mapper.outbound
  (:require [buddy.hashers :as hashers]
            [swarmpit.config :refer [config]]
            [swarmpit.uuid :refer [uuid]]))

(defn ->password
  [password]
  (hashers/derive password (config :password-hashing)))

(defn ->user
  [user]
  (assoc user :password (->password (:password user))))

(defn ->dockerhub
  [docker-user docker-user-info dockeruser-namespace]
  (let [full-name (:full_name docker-user-info)
        org-name (:orgname docker-user-info)]
    (assoc docker-user :name (if (empty? full-name)
                               org-name
                               full-name)
                       :role (:type docker-user-info)
                       :location (:location docker-user-info)
                       :company (:company docker-user-info)
                       :namespaces dockeruser-namespace)))