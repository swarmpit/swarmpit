(ns swarmpit.ui
  (:require [swarmpit.controller :as controller]
            [swarmpit.component.layout :as layout]
            [swarmpit.component.info :as info]))

(js/console.log "Swarmpit ...")

(layout/mount!)
(info/mount!)
(controller/start)