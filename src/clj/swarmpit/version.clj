(ns swarmpit.version
  (:require [swarmpit.config :as cfg]
            [clojure.java.io :as io]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.core.memoize :as memo]
            [swarmpit.api :as api])
  (:import (java.util Properties)))

(def pom-properties
  (doto (Properties.)
    (.load (-> "META-INF/maven/swarmpit/swarmpit/pom.properties"
               (io/resource)
               (io/reader)))))

(def initialized?
  (memo/ttl api/admin-exists? :ttl/threshold 1000))

(defn info
  []
  {:name        "swarmpit"
   :version     (get pom-properties "version")
   :revision    (get pom-properties "revision")
   :initialized (initialized?)
   :statistics  (some? (cfg/config :influxdb-url))
   :docker      {:api    (read-string (cfg/config :docker-api))
                 :engine (cfg/config :docker-engine)}})

(defn short-info
  "Used for routes in compile time"
  []
  {:name     "swarmpit"
   :version  (get pom-properties "version")
   :revision (get pom-properties "revision")})