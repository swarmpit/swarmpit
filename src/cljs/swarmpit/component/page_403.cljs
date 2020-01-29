(ns swarmpit.component.page-403
  (:require [rum.core :as rum]
            [sablono.core :refer-macros [html]]
            [material.components :as comp]
            [swarmpit.routes :as routes]))

(rum/defc form < rum/static []
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
              {:variant "h2"} (html [:span [:b "403"] " Forbidden"]))
            (html [:p "You are not authorized for selected action"])
            (comp/box
              {:className "Swarmpit-form-buttons"}
              (comp/button
                {:href    (routes/path-for-frontend :login)
                 :color   "default"
                 :variant "outlined"}
                "Go to login page"))))]])))
