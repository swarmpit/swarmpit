(ns swarmpit.docker.engine.cli
  (:require [clojure.java.shell :as shell]
            [cheshire.core :refer [parse-string]]))

(defn- login
  [username password]
  ["docker" "login" "--username" username "--password" password])

(defn- execute
  "Execute docker command and parse result"
  [cmd]
  (let [result (apply shell/sh cmd)]
    (if (= 0 (:exit result))
      (:out result)
      (:err result))))