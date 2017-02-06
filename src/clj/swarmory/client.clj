(ns swarmory.client
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string]]))

(def ^:private docker-host "unix:///var/run/docker.sock")

(defn get
  "Get docker data"
  []
  (let [{:keys [body error]} @(http/get docker-host)]
    (if error
      (println "Failed to [GET] %s, Reason: %s" docker-host error)
      (parse-string body true))))