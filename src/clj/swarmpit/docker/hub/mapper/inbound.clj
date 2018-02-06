(ns swarmpit.docker.hub.mapper.inbound)

(defn ->repository
  [repository]
  (into {:name        (:repo_name repository)
         :description (:short_description repository)
         :private     false}))

(defn ->repositories
  [repositories query page]
  (let [results (->> (:results repositories)
                     (map #(->repository %))
                     (map #(assoc % :id (hash (:name %))))
                     (into []))]
    {:query   query
     :page    page
     :limit   20
     :total   (:count repositories)
     :results results}))

(defn ->user-repository
  [repository]
  (into {:name        (str (:namespace repository) "/"
                           (:name repository))
         :description (:description repository)
         :private     (:is_private repository)}))

(defn ->user-repositories
  [repositories]
  (->> repositories
       (map #(->user-repository %))
       (map #(assoc % :id (hash (:name %))))
       (into [])))