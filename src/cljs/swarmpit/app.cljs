(ns swarmpit.app
  (:require [swarmpit.router :as router]))

(js/console.log "Swarmpit loaded !!!")
(js/console.log "Starting router ...")

(router/start)