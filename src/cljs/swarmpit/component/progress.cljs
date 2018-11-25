(ns swarmpit.component.progress
  (:require [material.component :as comp]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [loading? comp]
  (if loading?
    (html
      [:div {:style {:width          "100%"
                     :height         "100vh"
                     :display        "flex"
                     :alignItems     "center"
                     :justifyContent "center"}}
       [:div {:style {:maxWidth "50%"}} (comp/circular-progress {:size 100})]]) comp))