(ns swarmpit.ui
  (:require [swarmpit.controller :as controller]
            [swarmpit.component.layout :as layout]))

(.log js/console "Swarmpit ...")

(layout/mount!)
(controller/start)