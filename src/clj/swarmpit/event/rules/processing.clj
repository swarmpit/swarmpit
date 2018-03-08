(ns swarmpit.event.rules.processing
  (:refer-clojure :exclude [list])
  (:require [swarmpit.api :as api]))

;; Rules

(defprotocol Rule
  (match? [this event])
  (process [this event]))

(def cleanup-stackfiles
  (reify Rule
    (match? [_ event]
      (and (= "service" (:Type event))
           (= "remove" (:Action event))))
    (process [_ event]
      (doseq [stackfile (api/stackfiles)]
        (when (nil? (api/stack (:name stackfile)))
          (api/delete-stackfile (:name stackfile)))))))

(def list [cleanup-stackfiles])