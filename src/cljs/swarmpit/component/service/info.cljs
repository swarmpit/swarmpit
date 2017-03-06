(ns swarmpit.component.service.info
  (:require [swarmpit.material :as material]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-right
     (material/theme
       (material/raised-button
         #js {:href  "/#/services/create"
              :label "Edit"
              :style #js {:marginRight "12px"}}))
     (material/theme
       (material/raised-button
         #js {:href    "/#/services/create"
              :label   "Update"
              :primary true}))]]
   [:div]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))
