(ns user
  (:use [clojure.java.shell :only [sh]])
  (:require [ring.middleware.reload :refer [wrap-reload]]
            [figwheel-sidecar.repl-api :as figwheel]
            [swarmpit.api :as api]
            [swarmpit.server]))

;; Let Clojure warn you when it needs to reflect on types, or when it does math
;; on unboxed numbers. In both cases you should add type annotations to prevent
;; degraded performance.
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn- init-user
  []
  (api/create-user {:username "admin"
                    :password "admin"
                    :email    "admin@admin.com"
                    :role     "admin"}))

(defn- on-startup
  []
  (print (:out (sh "sh" "dev/script/init-db.sh")))
  (println (str "Swarmpit DB schema status: " (or (:reason (api/create-database))
                                                  "Database has been created.")))
  (println (str "Swarmpit DEV user status: " (if (some? (init-user))
                                               "Admin user has been created."
                                               "Admin user already exist."))))

(def http-handler
  (wrap-reload #'swarmpit.server/app))

(defn run []
  (on-startup)
  (figwheel/start-figwheel!))

(def browser-repl figwheel/cljs-repl)
