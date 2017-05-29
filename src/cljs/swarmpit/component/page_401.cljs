(ns swarmpit.component.page-401
  (:require [rum.core :as rum]))

(rum/defc not-authorized < rum/static []
  [:div.page-back
   [:div.page
    [:span
     [:h1 "401"]
     [:p "You are not authotized for selected action"]]]])

(defn mount!
  []
  (rum/mount (not-authorized) (.getElementById js/document "layout")))
