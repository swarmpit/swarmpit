(ns swarmpit.component.service.edit
  (:require [swarmpit.material :as material]
            [swarmpit.component.service.form-settings :as settings]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-volumes :as volumes]
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.service.form-deployment :as deployment]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [item]
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
         #js {:href  (str "/#/services/" (:id item))
              :label "Back"}))]]
   [:div.form-view
    [:div.form-view-group
     (material/form-view-section "General settings")
     (settings/form true)]
    [:div.form-view-group
     (material/form-view-section "Ports")
     (ports/form)]
    [:div.form-view-group
     (material/form-view-section "Volumes")
     (volumes/form)]
    [:div.form-view-group
     (material/form-view-section "Environment variables")
     (variables/form)]
    [:div.form-view-group
     (material/form-view-section "Deployment")
     (deployment/form)]]])

(defn- init-state
  [item]
  (reset! settings/state (select-keys item [:image :serviceName :mode :replicas]))
  (reset! ports/state (:ports item))
  (reset! volumes/state (:volumes item))
  (reset! variables/state (:variables item))
  (reset! deployment/state (:deployment item)))

(defn mount!
  [item]
  (init-state item)
  (rum/mount (form item) (.getElementById js/document "content")))