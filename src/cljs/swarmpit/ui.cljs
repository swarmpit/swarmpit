(ns swarmpit.ui
  (:require [swarmpit.controller :as controller]
            [swarmpit.component.layout :as layout]))

(js/console.log "Swarmpit ...")

(layout/mount!)

(controller/start)