(ns swarmpit.app
  (:require [swarmpit.component.layout :as layout]
            [swarmpit.component.service.create-form :as create-form]))

(.log js/console "Swarmpit ...")

(layout/mount!)
(create-form/mount!)
