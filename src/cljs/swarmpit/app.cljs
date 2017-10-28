(ns swarmpit.app
  (:require [swarmpit.router :as router]
            [swarmpit.component.layout :as layout]
            [swarmpit.component.message :as message]
            [swarmpit.eventsource :as eventsource]))

;; Starting router

(router/start)

;; Mounting layout

(layout/mount!)

;; Mounting message

(message/mount!)

;; Subscribe and listen for events

(eventsource/init!)