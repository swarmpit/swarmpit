(ns swarmpit.component.progress
  (:require [material.component :as comp]
            [material.component.form :as form]
            [rum.core :as rum]))

(enable-console-print!)

(def progress-style
  {:background "rgba(0, 0, 0, .2)"
   :position   "absolute"
   :display    "table"
   :textAlign  "center"
   :top        0
   :left       0
   :height     "100%"
   :width      "100%"
   :zIndex     10000})

(def progress-inner-style
  {:display       "table-cell"
   :verticalAlign "middle"})

(rum/defc progress < rum/static []
  (comp/mui
    (comp/circular-progress
      {:color      "#fff"
       :size       80
       :thickness  8
       :style      progress-style
       :innerStyle progress-inner-style})))

(rum/defc form < rum/static [loading? comp]
  (if loading?
    (progress)
    comp))

(rum/defc loading < rum/static []
  (form/loading true))

(rum/defc loaded < rum/static []
  (form/loading false))

(defn mount!
  []
  (rum/mount (progress) (.getElementById js/document "progress")))

(defn unmount!
  []
  (rum/unmount (.getElementById js/document "progress")))