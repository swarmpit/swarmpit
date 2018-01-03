(ns swarmpit.docker.utils
  "Utility ns for docker domain"
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

(defn trim-stack
  [stack name]
  "Removes stack name from object name eg. swarmpit_app -> app"
  (if (some? stack)
    (str/replace name #"^.*_" "")
    name))


