(ns swarmpit.app
  (:require [swarmpit.router :as router]
            [swarmpit.component.layout :as layout]
            [swarmpit.component.message :as message]
            [material.components :as comp]))

;; Apply stored theme on load
(set! (-> js/document .-documentElement .-className) (comp/current-theme-mode))

;; Starting router

(router/start)

;; Mounting layout

(layout/mount!)

;; Mounting message

(message/mount!)
