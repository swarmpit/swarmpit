(ns swarmpit.docker.hub.mapper.inbound)

(defn ->repository
  [repository]
  (into {:name        (:repo_name repository)
         :description (:short_description repository)
         :private     false
         :stars       (:star_count repository)
         :pulls       (:pull_count repository)
         :official    (:is_official repository)}))

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
         :private     (:is_private repository)
         :stars       (:star_count repository)
         :pulls       (:pull_count repository)
         :official    (:is_official repository)}))

(defn ->user-repositories
  [repositories]
  (->> repositories
       (map #(->user-repository %))
       (map #(assoc % :id (hash (:name %))))
       (into [])))