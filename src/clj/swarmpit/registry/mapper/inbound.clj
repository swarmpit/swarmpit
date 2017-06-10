(ns swarmpit.registry.mapper.inbound)

(defn ->dockerhub-repositories
  [repositories query page]
  (let [results (->> (:results repositories)
                     (map #(assoc % :id (hash (:repo_name %))))
                     (into []))]
    {:query   query
     :page    page
     :limit   20
     :total   (:count repositories)
     :results results}))

(defn ->repositories
  [repositories]
  (->> repositories
       (map (fn [repo] (into {:id   (hash repo)
                              :name repo})))))