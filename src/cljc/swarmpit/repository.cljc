(ns swarmpit.repository
  "Utility ns for docker repository"
  (:require [clojure.string :as str]))

(defn namespace?
  [repository]
  (str/includes? repository "/"))


