(ns swarmpit.event.rules.processing
  (:refer-clojure :exclude [list])
  (:require [swarmpit.api :as api]
            [swarmpit.stats :as stats]
            [swarmpit.event.rules.predicate :refer :all]))

;; Rules

(defprotocol Rule
  (match? [this type message])
  (process [this message]))

(def update-stats
  (reify Rule
    (match? [_ type message]
      (stats? type))
    (process [_ message]
      (stats/store-to-cache message)
      (stats/store-to-db message))))

(def list [update-stats])