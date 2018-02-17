(ns swarmpit.couchdb.migration
  (:require [swarmpit.couchdb.client :as db]
            [swarmpit.api :as api]
            [swarmpit.uuid :refer [uuid]]))

(defn- create-secret
  []
  (db/create-secret {:secret (uuid)})
  (println "Default token secret created"))

(defn- create-admin
  []
  (api/create-user {:username "admin"
                    :password "admin"
                    :email    "admin@admin.com"
                    :role     "admin"})
  (println "Default admin user created"))

(defn- verify-initial-data
  []
  (when (nil? (db/get-secret))
    (create-secret))
  (when (empty? (db/users))
    (create-admin)))

(defn- upgrade-registry
  [reg]
  (if (= "registry" (:type reg))
    (-> reg
        (merge {:withAuth (:isPrivate reg)
                :url      (str (:scheme reg) "://" (:url reg))})
        (dissoc :version :scheme :isPrivate))
    reg))

(defn- add-registry-owners
  []
  (let [admin (->> (db/user-by-username "admin")
                   (list)
                   (concat (db/users))
                   (filter #(= "admin" (:role %)))
                   (first)
                   :username)]
    (->> ["registry" "dockeruser"]
         (mapcat #(db/find-docs {:owner {:$exists false}} %))
         (map #(merge % {:owner  admin
                         :public true}))
         (map upgrade-registry)
         (map db/update-doc))))

(def migrations
  {:initial         verify-initial-data
   :registry-owners add-registry-owners})

(defn migrate
  []
  (doseq [migration (->> (apply dissoc migrations (db/migrations))
                         (into []))]
    (db/record-migration (key migration) ((val migration)))))