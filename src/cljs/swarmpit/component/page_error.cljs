(ns swarmpit.component.page-error
  (:require [rum.core :as rum]))

(rum/defc form < rum/static []
  [:div.page-back
   [:div.page
    [:span
     [:h1 "500"]
     [:p "Ups something goes wrong..."]]]])