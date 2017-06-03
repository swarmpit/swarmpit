(ns swarmpit.registry.mapper.inbound)

(defn ->v1-repositories
  [repositories]
  (let [results (->> (:results repositories)
                     (map #(assoc % :id (hash (:name %))))
                     (into []))]
    (merge (select-keys repositories [:page :query])
           {:limit   (:page_size repositories)
            :total   (:num_results repositories)
            :results results})))

(defn ->v2-repositories
  [repositories]
  (->> repositories
       (map (fn [repo] (into {:id   (hash repo)
                              :name repo})))))