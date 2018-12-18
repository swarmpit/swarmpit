(ns swarmpit.component.page-error
  (:require [rum.core :as rum]
            [sablono.core :refer-macros [html]]))

(rum/defc form < rum/reactive [{{:keys [stacktrace]} :params}]
  [:div.page-back
   [:div.page
    [:div.page-form
     [:span
      [:h1 "500"]
      [:p "Ups something goes wrong..."]
      [:p stacktrace]]]]])