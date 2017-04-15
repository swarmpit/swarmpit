(ns swarmpit.component.service.edit
  (:require [swarmpit.material :as material]
            [swarmpit.component.service.form-settings :as settings]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-variables :as variables]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [item]
  (let [id (get item "ID")]
    [:div
     [:div.form-panel
      [:div.form-panel-right
       (material/theme
         (material/raised-button
           #js {:href    "/#/services/create"
                :label   "Save"
                :primary true
                :style   #js {:marginRight "12px"}}))
       (material/theme
         (material/raised-button
           #js {:href  (str "/#/services/" id)
                :label "Back"}))]]
     [:div.form-view
      [:div.form-view-group
       (material/form-view-section "General settings")
       (settings/form true)]
      [:div.form-view-group
       (material/form-view-section "Ports")
       (ports/form)]
      [:div.form-view-group
       (material/form-view-section "Environment variables")
       (variables/form)]]]))

(defn- init-state
  [item]
  (reset! settings/state (select-keys item [:image :serviceName :mode :replicas :autoredeploy]))
  (reset! ports/state (:ports item))
  (reset! variables/state (:variables item)))

(defn mount!
  [item]
  (init-state item)
  (rum/mount (form item) (.getElementById js/document "content")))
