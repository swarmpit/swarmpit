(ns swarmpit.event.processor
  (:require [clojure.core.async :refer [go]]
            [swarmpit.event.rules.processing :as rule]))

(defn process
  [event]
  "Process event on server"
  (go
    (doseq [rule (filter #(rule/match? % event) rule/list)]
      (rule/process rule event))))
