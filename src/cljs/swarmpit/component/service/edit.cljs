(ns swarmpit.component.service.edit
  (:require [swarmpit.material :as material]
            [swarmpit.router :as router]
            [swarmpit.component.service.form-settings :as settings]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-volumes :as volumes]
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.service.form-deployment :as deployment]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [rum.core :as rum]
            [ajax.core :refer [POST]]))

(enable-console-print!)

(defn- update-service-handler
  [service-id]
  (POST (str "/services/" service-id)
        {:format        :json
         :params        (-> @settings/state
                            (assoc :ports @ports/state)
                            (assoc :volumes @volumes/state)
                            (assoc :variables @variables/state)
                            (assoc :deployment @deployment/state))
         :finally       (progress/mount!)
         :handler       (fn [_]
                          (let [message (str "Service " service-id " has been updated.")]
                            (progress/unmount!)
                            (router/dispatch! (str "/#/services/" service-id))
                            (message/mount! message)))
         :error-handler (fn [{:keys [status status-text]}]
                          (let [message (str "Service update failed. Status: " status " Reason: " status-text)]
                            (progress/unmount!)
                            (message/mount! message)))}))

(rum/defc form < rum/static [item]
  (let [id (:id item)]
    [:div
     [:div.form-panel
      [:div.form-panel-right
       (material/theme
         (material/raised-button
           #js {:onTouchTap #(update-service-handler id)
                :label      "Save"
                :primary    true
                :style      #js {:marginRight "12px"}}))
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
       (material/form-view-section "Volumes")
       (volumes/form)]
      [:div.form-view-group
       (material/form-view-section "Environment variables")
       (variables/form)]
      [:div.form-view-group
       (material/form-view-section "Deployment")
       (deployment/form)]]]))

(defn- init-state
  [item]
  (reset! settings/state (select-keys item [:image :version :serviceName :mode :replicas]))
  (reset! ports/state (:ports item))
  (reset! volumes/state (:volumes item))
  (reset! variables/state (:variables item))
  (reset! deployment/state (:deployment item)))

(defn mount!
  [item]
  (init-state item)
  (rum/mount (form item) (.getElementById js/document "content")))