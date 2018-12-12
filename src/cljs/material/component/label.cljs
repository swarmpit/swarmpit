(ns material.component.label
  (:refer-clojure :exclude [update])
  (:require [sablono.core :refer-macros [html]]))

(defn base
  [label color]
  (html
    [:span {:class (str "Swarmpit-label Swarmpit-label-" color)
            :key   (str "label-" label)} label]))

(defn green
  [label]
  (base label "green"))

(defn pulsing
  [label]
  (base label "pulsing"))

(defn info
  [label]
  (base label "info"))

(defn grey
  [label]
  (base label "grey"))

(defn red
  [label]
  (base label "red"))

(defn yellow
  [label]
  (base label "yellow"))

(defn blue
  [label]
  (base label "blue"))

(defn primary
  [label]
  (base label "primary"))
