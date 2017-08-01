(ns swarmpit.app
  (:require [swarmpit.router :as router]
            [swarmpit.component.layout :as layout]
            [swarmpit.component.message :as message]
            [swarmpit.component.network.create :as network]
            [swarmpit.component.volume.create :as volume]))

;; Starting router

(router/start)

;; Mounting layout

(layout/mount!)

;; Mounting message

(message/mount!)

;; Fetch plugins data

(network/network-plugin-handler)
(volume/volume-plugin-handler)