(ns swarmpit.event.processor
  (:require [clojure.core.async :refer [go]]
            [swarmpit.event.rules.processing :as rule]))

(defn process
  [{:keys [type message] :as event}]
  "Process event on server"
  (go
    (doseq [rule (filter #(rule/match? % type message) rule/list)]
      (rule/process rule message))))
