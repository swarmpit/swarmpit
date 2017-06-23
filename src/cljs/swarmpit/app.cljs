(ns swarmpit.app
  (:require [swarmpit.router :as router]
            [swarmpit.component.layout :as layout]))

;; Starting router

(router/start)

;; Mounting layout

(layout/mount!)