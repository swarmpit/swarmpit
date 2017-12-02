(ns material.component.label
  (:refer-clojure :exclude [update])
  (:require [sablono.core :refer-macros [html]]))

(defn red
  [text]
  (html [:span.label.label-red text]))

(defn yellow
  [text]
  (html [:span.label.label-yellow text]))

(defn green
  [text]
  (html [:span.label.label-green text]))

(defn blue
  [text]
  (html [:span.label.label-blue text]))

(defn grey
  [text]
  (html [:span.label.label-grey text]))

(defn update
  [text]
  (html [:span.label.label-update text]))

(defn info
  [text]
  (html [:span.label.label-info text]))
