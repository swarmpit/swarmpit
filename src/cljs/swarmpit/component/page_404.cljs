(ns swarmpit.component.page-404
  (:require [rum.core :as rum]
            [sablono.core :refer-macros [html]]
            [material.components :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]))

(enable-console-print!)

(rum/defc form < rum/static []
  (let [params (state/get-value [:route :params])
        id (or (get-in params [:origin :params :id])
               (get-in params [:origin :params :name]))
        message (or (get-in params [:error :error])
                    "What you are looking for, I do not have.")]
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
                {:variant "h2"} (html [:span [:b "404"] " Not Found"]))
              (html [:p message])
              (comp/box
                {:className "Swarmpit-form-buttons"}
                (if (some? params)
                  (comp/button
                    {:onClick #(.back js/window.history)
                     :color   "default"
                     :variant "outlined"}
                    "Go back")
                  (comp/button
                    {:href    (routes/path-for-frontend :index)
                     :color   "default"
                     :variant "outlined"}
                    "Go home")))))]]))))