(ns swarmpit.component.service.edit
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.service.form-settings :as settings]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-networks :as networks]
            [swarmpit.component.service.form-mounts :as mounts]
            [swarmpit.component.service.form-secrets :as secrets]
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.service.form-labels :as labels]
            [swarmpit.component.service.form-deployment :as deployment]
            [swarmpit.component.service.form-deployment-placement :as placement]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn render-item
  [val]
  (if (boolean? val)
    (comp/checkbox {:checked val})
    val))

(defn- update-service-handler
  [service-id]
  (let [settings (state/get-value settings/cursor)
        ports (state/get-value ports/cursor)
        networks (state/get-value networks/cursor)
        mounts (state/get-value mounts/cursor)
        secrets (state/get-value secrets/cursor)
        variables (state/get-value variables/cursor)
        labels (state/get-value labels/cursor)
        deployment (state/get-value deployment/cursor)]
    (handler/post
      (routes/path-for-backend :service-update {:id service-id})
      {:params     (-> settings
                       (assoc :ports ports)
                       (assoc :networks networks)
                       (assoc :mounts mounts)
                       (assoc :secrets secrets)
                       (assoc :variables variables)
                       (assoc :labels labels)
                       (assoc :deployment deployment))
       :on-success (fn [_]
                     (dispatch!
                       (routes/path-for-frontend :service-info {:id service-id}))
                     (message/info
                       (str "Service " service-id " has been updated.")))
       :on-error   (fn [response]
                     (message/error
                       (str "Service update failed. Reason: " (:error response))))})))

(defn- init-state
  [service networks]
  (state/set-value (select-keys service [:registry :repository :version :serviceName :mode :replicas]) settings/cursor)
  (state/set-value (:ports service) ports/cursor)
  (state/set-value (->> networks
                        (map #(select-keys % [:networkName]))
                        (into [])) networks/cursor)
  (state/set-value (:mounts service) mounts/cursor)
  (state/set-value (->> (:secrets service)
                        (map #(select-keys % [:secretName]))
                        (into [])) secrets/cursor)
  (state/set-value (:variables service) variables/cursor)
  (state/set-value (:labels service) labels/cursor)
  (state/set-value (:deployment service) deployment/cursor))

(def init-state-mixin
  (mixin/init
    (fn [{:keys [service networks]}]
      (init-state service networks)
      (mounts/volumes-handler)
      (networks/networks-handler)
      (secrets/secrets-handler)
      (placement/placement-handler))))

(rum/defc form-settings < rum/static []
  [:div.form-service-edit-group
   (comp/form-section "General settings")
   (settings/form true)])

(rum/defc form-ports < rum/static []
  [:div.form-service-edit-group.form-service-group-border
   (comp/form-section-add "Ports" ports/add-item)
   (ports/form-update)])

(rum/defc form-networks < rum/static []
  [:div.form-service-edit-group.form-service-group-border
   (comp/form-section-add "Networks" networks/add-item)
   (networks/form-update)])

(rum/defc form-mounts < rum/static []
  [:div.form-service-edit-group.form-service-group-border
   (comp/form-section-add "Mounts" mounts/add-item)
   (mounts/form-update)])

(rum/defc form-secrets < rum/static []
  [:div.form-service-edit-group.form-service-group-border
   (if (empty? @secrets/secrets)
     (comp/form-section "Secrets")
     (comp/form-section-add "Secrets" secrets/add-item))
   (secrets/form-update)])

(rum/defc form-variables < rum/static []
  [:div.form-service-edit-group.form-service-group-border
   (comp/form-section-add "Environment variables" variables/add-item)
   (variables/form-update)])

(rum/defc form-labels < rum/static []
  [:div.form-service-edit-group.form-service-group-border
   (comp/form-section-add "Labels" labels/add-item)
   (labels/form-update)])

(rum/defc form-deployment < rum/static []
  [:div.form-service-edit-group.form-service-group-border
   (comp/form-section "Deployment")
   (deployment/form)])

(rum/defc form < rum/static
                 init-state-mixin [data]
  (let [{:keys [service]} data]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/services
                        (:serviceName service))]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:onTouchTap #(update-service-handler (:id service))
            :label      "Save"
            :primary    true}))
       [:span.form-panel-delimiter]
       (comp/mui
         (comp/raised-button
           {:href  (routes/path-for-frontend :service-info (select-keys service [:id]))
            :label "Back"}))]]
     [:div.form-service-edit
      (form-settings)
      (form-ports)
      (form-networks)
      (form-mounts)
      (form-secrets)
      (form-variables)
      (form-labels)
      (form-deployment)]]))