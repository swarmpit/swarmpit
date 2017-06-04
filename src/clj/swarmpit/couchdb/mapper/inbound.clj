(ns swarmpit.couchdb.mapper.inbound)

(defn ->registries
  [registries]
  (->> registries
       (map #(assoc % :id (hash (:name %))))
       (into [])))