(ns swarmpit.component.progress
  (:require [material.components :as comp]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [loading? comp]
  (if loading?
    (html
      [:div
       {:style {:width          "100%"
                :height         "100vh"
                :display        "flex"
                :alignItems     "center"
                :justifyContent "center"}}
       [:div
        {:style {:maxWidth "50%"}}
        (comp/mui (comp/circular-progress {:size 100}))]]) comp))