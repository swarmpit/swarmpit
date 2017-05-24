(ns swarmpit.component.page-404
  (:require [rum.core :as rum]))

(rum/defc not-found < rum/static []
  [:div.page-back
   [:div.page
    [:span
     [:h1 "404"]
     [:p "What you are looking for, "]
     [:p "I do not have."]]]])

(defn mount!
  []
  (rum/mount (not-found) (.getElementById js/document "layout")))
