(ns swarmpit.app
  (:require [swarmpit.router :as router]
            [swarmpit.storage :as storage]
            [swarmpit.component.layout :as layout]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [material.components :as comp]))

;; Initialize theme from localStorage
(let [saved-theme (or (storage/get "theme") "light")]
  (reset! comp/theme-mode saved-theme)
  (state/update-value [:theme] saved-theme state/layout-cursor))

;; Starting router

(router/start)

;; Mounting layout

(layout/mount!)

;; Mounting message

(message/mount!)
