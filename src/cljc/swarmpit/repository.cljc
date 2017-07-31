(ns swarmpit.repository
  "Utility ns for docker repository"
  (:require [clojure.string :as str]))

(defn namespace?
  [repository]
  (str/includes? repository "/"))

(defn add-dockerhub-namespace
  "Prefix dockerhub repository with default namespace if missing."
  [repository]
  (if (namespace? repository)
    repository
    (str "library/" repository)))


