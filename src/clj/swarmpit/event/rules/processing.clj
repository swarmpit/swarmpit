(ns swarmpit.event.rules.processing
  (:refer-clojure :exclude [list])
  (:require [swarmpit.api :as api]
            [swarmpit.stats :as stats]
            [swarmpit.event.rules.predicate :refer :all]))

;; Rules

(defprotocol Rule
  (match? [this type message])
  (process [this message]))

(def cleanup-stackfiles
  (reify Rule
    (match? [_ type message]
      (and (event? type)
           (service-remove-event? message)))
    (process [_ message]
      (doseq [{:keys [name]} (api/stackfiles)]
        (when (nil? (api/stack name))
          (try
            (api/delete-stackfile name)
            (catch Exception _)))))))

(def update-stats
  (reify Rule
    (match? [_ type message]
      (stats? type))
    (process [_ message]
      (stats/create message))))

(def list [cleanup-stackfiles
           update-stats])