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
      (doseq [{:keys [name]} (api/stackfiles)]
        (when (and (nil? (api/stack name))
                   (some? (api/stackfile name)))
          (api/delete-stackfile name))))))

(def list [cleanup-stackfiles])