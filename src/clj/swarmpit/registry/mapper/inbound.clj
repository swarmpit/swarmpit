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
      :history
      (first)
      :v1Compatibility
      (parse-string true)))

(defn ->repository-without-prefix
  [image-name]
  (let [separator-pos (str/index-of image-name "/")
        length (count image-name)]
    (subs image-name (inc separator-pos) length)))