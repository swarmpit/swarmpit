(ns swarmpit.docker.secret
  (:refer-clojure :exclude [get])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private secrets-path "/run/secrets")

(defn get
  "Get secret value from Docker secrets directory"
  [secret-name]
  (try
    (-> (str secrets-path "/" secret-name)
        (slurp)
        (str/trim))
    (catch Exception _
      nil)))
