(ns swarmpit.ui
  (:require [swarmpit.component.layout :as layout]
            [swarmpit.component.service.form-create :as form]))

(.log js/console "Swarmpit ...")

(layout/mount!)
(form/mount!)
