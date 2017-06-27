(ns swarmpit.component.page-404
  (:require [rum.core :as rum]))

(rum/defc form < rum/static []
  [:div.page-back
   [:div.page
    [:div.page-form
     [:span
      [:h1 "404"]
      [:p "What you are looking for, I do not have."]]]]])
