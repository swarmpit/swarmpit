(ns swarmpit.component.service.edit
  (:require [material.component :as comp]
            [swarmpit.uri :refer [dispatch!]]
            [swarmpit.component.state :as state]
            [swarmpit.component.service.form-settings :as settings]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-volumes :as volumes]
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.service.form-deployment :as deployment]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(defn- update-service-handler
  [service-id]
  (let [settings (state/get-value settings/cursor)
        ports (state/get-value ports/cursor)
        volumes (state/get-value volumes/cursor)
        variables (state/get-value variables/cursor)
        deployment (state/get-value deployment/cursor)]
    (ajax/POST (str "/services/" service-id)
               {:format        :json
                :params        (-> settings
                                   (assoc :ports ports)
                                   (assoc :volumes volumes)
                                   (assoc :variables variables)
                                   (assoc :deployment deployment))
                :finally       (progress/mount!)
                :handler       (fn [_]
                                 (let [message (str "Service " service-id " has been updated.")]
                                   (progress/unmount!)
                                   (dispatch! (str "/#/services/" service-id))
                                   (message/mount! message)))
                :error-handler (fn [{:keys [status status-text]}]
                                 (let [message (str "Service update failed. Status: " status " Reason: " status-text)]
                                   (progress/unmount!)
                                   (message/mount! message)))})))

(rum/defc form < rum/static [item]
  (let [id (:id item)]
    [:div
     [:div.form-panel
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:onTouchTap #(update-service-handler id)
            :label      "Save"
            :primary    true
            :style      {:marginRight "12px"}}))
       (comp/mui
         (comp/raised-button
           {:href  (str "/#/services/" id)
            :label "Back"}))]]
     [:div.form-view
      [:div.form-view-group
       (comp/form-view-section "General settings")
       (settings/form true)]
      [:div.form-view-group
       (comp/form-view-section "Ports")
       (ports/form)]
      [:div.form-view-group
       (comp/form-view-section "Volumes")
       (volumes/form)]
      [:div.form-view-group
       (comp/form-view-section "Environment variables")
       (variables/form)]
      [:div.form-view-group
       (comp/form-view-section "Deployment")
       (deployment/form)]]]))

(defn- init-state
  [item]
  (state/set-value (select-keys item [:image :version :serviceName :mode :replicas]) settings/cursor)
  (state/set-value (:ports item) ports/cursor)
  (state/set-value (:volumes item) volumes/cursor)
  (state/set-value (:variables item) variables/cursor)
  (state/set-value (:deployment item) deployment/cursor))

(defn mount!
  [item]
  (init-state item)
  (rum/mount (form item) (.getElementById js/document "content")))