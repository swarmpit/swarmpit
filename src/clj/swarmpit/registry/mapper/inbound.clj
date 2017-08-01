(ns swarmpit.registry.mapper.inbound
  (:require [cheshire.core :refer [parse-string]]
            [clojure.string :as str]))

(defn ->repositories
  [repositories]
  (->> repositories
       (map (fn [repo] (into {:id   (hash repo)
                              :name repo})))))

(defn ->repository-config
  [manifest]
  (-> manifest
      (get "history")
      (first)
      (get "v1Compatibility")
      (parse-string true)))