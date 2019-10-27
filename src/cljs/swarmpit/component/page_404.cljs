(ns swarmpit.component.page-404
  (:require [rum.core :as rum]
            [sablono.core :refer-macros [html]]
            [material.components :as comp]
            [swarmpit.component.common :as common]
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
          [:div.Swarmpit-form-paper
           (common/edit-title "Not Found" id)
           [:div.Swarmpit-user-form
            (comp/grid
              {:container true
               :className "Swarmpit-form-main-grid"
               :spacing   24}
              (comp/grid
                {:item true
                 :xs   12}
                (html [:p message]))
              (comp/grid
                {:item true
                 :xs   12}
                (html
                  [:div.Swarmpit-form-buttons
                   (if (some? params)
                     (comp/button
                       {:onClick #(.back js/window.history)
                        :color   "outlined"
                        :variant "contained"}
                       "Go back")
                     (comp/button
                       {:href    (routes/path-for-frontend :index)
                        :color   "outlined"
                        :variant "contained"}
                       "go home"))])))]]]]))))
