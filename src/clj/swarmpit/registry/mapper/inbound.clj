(ns swarmpit.registry.mapper.inbound
  (:require [cheshire.core :refer [parse-string]]))

(defn ->repositories
  [repositories]
  (->> repositories
       (map (fn [repo] (into {:id   (hash repo)
                              :name repo})))))

(defn ->repository-config
  [manifest]
  (-> manifest
      :history
      (first)
      :v1Compatibility
      (parse-string true)))