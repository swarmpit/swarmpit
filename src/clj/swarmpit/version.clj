(ns swarmpit.version
  (:require [swarmpit.config :as cfg]
            [clojure.java.io :as io]
            [clojure.walk :refer [keywordize-keys]])
  (:import (java.util Properties)))

(def pom-properties
  (doto (Properties.)
    (.load (-> "META-INF/maven/swarmpit/swarmpit/pom.properties"
               (io/resource)
               (io/reader)))))

(defn info
  []
  {:name     "swarmpit"
   :version  (get pom-properties "version")
   :revision (get pom-properties "revision")
   :docker   {:api    (read-string (cfg/config :docker-api))
              :engine (cfg/config :docker-engine)}})