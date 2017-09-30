(ns swarmpit.version
  (:require [clojure.java.io :as io]
            [clojure.walk :refer [keywordize-keys]])
  (:import (java.util Properties)))

(def pom-properties
  (doto (Properties.)
    (.load (-> "META-INF/maven/swarmpit/swarmpit/pom.properties"
               (io/resource)
               (io/reader)))))

(def version
  {:name     "swarmpit"
   :version  (get pom-properties "version")
   :revision (get pom-properties "revision")})