(ns swarmpit.component.page-error
  (:require [rum.core :as rum]
            [sablono.core :refer-macros [html]]
            [material.components :as comp]))

(rum/defc form < rum/static [{{:keys [stacktrace]} :params}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (comp/container
          {:maxWidth  "sm"
           :className "Swarmpit-container"}
          (comp/box
            {:className "Swarmpit-404"}
            (comp/typography
              {:variant "h2"} (html [:span [:b "500"] " Internal Server Error"]))
            (html [:p "Ups something went wrong..."])
            (html [:p stacktrace])))]])))