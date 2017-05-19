(ns swarmpit.component.page-404
  (:require [rum.core :as rum]))

(rum/defc not-found < rum/static []
  [:div.login-back
   [:div.login
    [:span
     [:h1 "404"]
     [:p "What you are looking for, "]
     [:p "I do not have."]]]])

(defn mount!
  []
  (rum/mount (not-found) (.getElementById js/document "layout")))
