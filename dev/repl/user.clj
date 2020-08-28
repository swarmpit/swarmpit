(ns repl.user
  (:require [ring.middleware.reload :refer [wrap-reload]]
            [clojure.java.shell :refer [sh]]
            [figwheel-sidecar.repl-api :as f]
            [swarmpit.setup :as setup]
            [swarmpit.database :as db]
            [swarmpit.agent :as agent]
            [swarmpit.config :as cfg]
            [swarmpit.server]))

;; Let Clojure warn you when it needs to reflect on types, or when it does math
;; on unboxed numbers. In both cases you should add type annotations to prevent
;; degraded performance.
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

;; user is a namespace that the Clojure runtime looks for and
;; loads if its available

;; You can place helper functions in here. This is great for starting
;; and stopping your webserver and other development services

;; The definitions in here will be available if you run "lein repl" or launch a
;; Clojure repl some other way

;; You have to ensure that the libraries you :require are listed in your dependencies

;; Once you start down this path
;; you will probably want to look at
;; tools.namespace https://github.com/clojure/tools.namespace
;; and Component https://github.com/stuartsierra/component

(def http-handler
  (wrap-reload #'swarmpit.server/app))

(defn- on-startup
  "Run before figwheel start"
  []
  (print (:out (sh "sh" "dev/script/init-db.sh")))
  (print (:out (sh "sh" "dev/script/init-agent.sh")))
  (print (:out (sh "sh" "dev/script/init-influx.sh")))
  (cfg/update! {:agent-url "http://localhost:8888"})
  (cfg/update! {:influxdb-url "http://localhost:8086"})
  (db/init)
  (agent/init)
  (setup/docker)
  (setup/log))

(defn fig-start
  "This starts the figwheel server and watch based auto-compiler."
  []
  ;; this call will only work are long as your :cljsbuild and
  ;; :figwheel configurations are at the top level of your project.clj
  ;; and are not spread across different lein profiles

  ;; otherwise you can pass a configuration into start-figwheel! manually
  (on-startup)
  (f/start-figwheel!))

(defn fig-stop
  "Stop the figwheel server and watch based auto-compiler."
  []
  (f/stop-figwheel!))

;; if you are in an nREPL environment you will need to make sure you
;; have setup piggieback for this to work
(defn cljs-repl
  "Launch a ClojureScript REPL that is connected to your build and host environment."
  []
  (f/cljs-repl "dev"))