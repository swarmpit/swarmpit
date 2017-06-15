(ns swarmpit.component.page-401
  (:require [swarmpit.routes :as routes]
            [rum.core :as rum]))

(rum/defc not-authorized < rum/static []
  [:div.page-back
   [:div.page
    [:span
     [:h1 "401"]
     [:p "You are not authotized for selected action"]
     [:p "Go to login " [:a {:href (routes/path-for-frontend :login)} "page"]]]]])

(defn mount!
  []
  (rum/mount (not-authorized) (.getElementById js/document "layout")))
