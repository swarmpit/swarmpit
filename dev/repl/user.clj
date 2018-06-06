(ns repl.user
  (:require [ring.middleware.reload :refer [wrap-reload]]
            [clojure.java.shell :refer [sh]]
            [figwheel-sidecar.repl-api :as figwheel]
            [swarmpit.setup :as setup]
            [swarmpit.database :as db]
            [swarmpit.agent :as agent]
            [swarmpit.server]))

;; Let Clojure warn you when it needs to reflect on types, or when it does math
;; on unboxed numbers. In both cases you should add type annotations to prevent
;; degraded performance.
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn- on-startup
  []
  (print (:out (sh "sh" "dev/script/init-db.sh")))
  (print (:out (sh "sh" "dev/script/init-agent.sh")))
  (db/init)
  (agent/init)
  (setup/docker))

(def http-handler
  (wrap-reload #'swarmpit.server/app))

(defn run []
  (on-startup)
  (figwheel/start-figwheel!))

(def browser-repl figwheel/cljs-repl)
