(ns swarmpit.app
  (:require [swarmpit.router :as router]
            [swarmpit.component.layout :as layout]
            [swarmpit.component.message :as message]))

;; Starting router

(router/start)

;; Mounting layout

(layout/mount!)

;; Mounting message

(message/mount!)
