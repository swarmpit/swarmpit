(ns swarmpit.component.page-error
  (:require [rum.core :as rum]))

(rum/defc not-found < rum/static []
  [:div.page-back
   [:div.page
    [:span
     [:h1 "500"]
     [:p "Ups something goes wrong..."]]]])

(defn mount!
  []
  (rum/mount (not-found) (.getElementById js/document "layout")))