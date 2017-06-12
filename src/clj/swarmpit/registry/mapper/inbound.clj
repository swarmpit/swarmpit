(ns swarmpit.registry.mapper.inbound)

(defn ->repositories
  [repositories]
  (->> repositories
       (map (fn [repo] (into {:id   (hash repo)
                              :name repo})))))