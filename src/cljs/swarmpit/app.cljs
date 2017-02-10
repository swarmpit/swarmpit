(ns swarmpit.app
  (:require [swarmpit.component.layout :as layout]))

(.log js/console "Swarmpit ...")

(layout/mount!)
