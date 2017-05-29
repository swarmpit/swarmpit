(ns swarmpit.component.page-error
  (:require [rum.core :as rum]))

(rum/defc error < rum/static []
  [:div.page-back
   [:div.page
    [:span
     [:h1 "500"]
     [:p "Ups something goes wrong..."]]]])

(defn mount!
  []
  (rum/mount (error) (.getElementById js/document "layout")))