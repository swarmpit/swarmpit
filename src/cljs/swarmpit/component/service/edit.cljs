(ns swarmpit.component.service.edit
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.state :as state]
            [swarmpit.component.service.form-settings :as settings]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-networks :as networks]
            [swarmpit.component.service.form-mounts :as mounts]
            [swarmpit.component.service.form-secrets :as secrets]
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.service.form-deployment :as deployment]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(defn- update-service-info-msg
  [id]
  (str "Service " id " has been updated."))

(defn- update-service-error-msg
  [error]
  (str "Service update failed. Reason: " error))

(defn- update-service-handler
  [service-id]
  (let [settings (state/get-value settings/cursor)
        ports (state/get-value ports/cursor)
        networks (state/get-value networks/cursor)
        mounts (state/get-value mounts/cursor)
        secrets (state/get-value secrets/cursor)
        variables (state/get-value variables/cursor)
        deployment (state/get-value deployment/cursor)]
    (ajax/POST (routes/path-for-backend :service-update {:id service-id})
               {:format        :json
                :headers       {"Authorization" (storage/get "token")}
                :params        (-> settings
                                   (assoc :ports ports)
                                   (assoc :networks networks)
                                   (assoc :mounts mounts)
                                   (assoc :secrets secrets)
                                   (assoc :variables variables)
                                   (assoc :deployment deployment))
                :finally       (progress/mount!)
                :handler       (fn [_]
                                 (progress/unmount!)
                                 (dispatch!
                                   (routes/path-for-frontend :service-info {:id service-id}))
                                 (message/mount!
                                   (update-service-info-msg service-id)))
                :error-handler (fn [{:keys [response]}]
                                 (let [error (get-in response ["error"])]
                                   (progress/unmount!)
                                   (message/mount!
                                     (update-service-error-msg error) true)))})))

(defn render-item
  [val]
  (if (boolean? val)
    (comp/checkbox {:checked val})
    val))

(rum/defc form < rum/static [item volumes secrets]
  (let [id (:id item)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/services
                        (:serviceName item))]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:onTouchTap #(update-service-handler id)
            :label      "Save"
            :primary    true}))
       [:span.form-panel-delimiter]
       (comp/mui
         (comp/raised-button
           {:href  (str "/#/services/" id)
            :label "Back"}))]]
     [:div.form-view
      [:div.form-view-group
       (comp/form-section "General settings")
       (settings/form true)]
      [:div.form-view-group
       (comp/form-section-add "Ports" ports/add-item)
       (ports/form-update)]
      [:div.form-view-group
       (comp/form-section "Networks")
       (networks/form-view (:networks item))]
      [:div.form-view-group
       (comp/form-section-add "Mounts" mounts/add-item)
       (mounts/form-update volumes)]
      [:div.form-view-group
       (if (empty? secrets)
         (comp/form-section "Secrets")
         (comp/form-section-add "Secrets" secrets/add-item))
       (secrets/form-update secrets)]
      [:div.form-view-group
       (comp/form-section-add "Environment variables" variables/add-item)
       (variables/form-update)]
      [:div.form-view-group
       (comp/form-section "Deployment")
       (deployment/form)]]]))

(defn- init-state
  [item]
  (state/set-value (select-keys item [:repository :version :serviceName :mode :replicas]) settings/cursor)
  (state/set-value (:ports item) ports/cursor)
  (state/set-value (:networks item) networks/cursor)
  (state/set-value (:mounts item) mounts/cursor)
  (state/set-value (:secrets item) secrets/cursor)
  (state/set-value (:variables item) variables/cursor)
  (state/set-value (:deployment item) deployment/cursor))

(defn mount!
  [item volumes secrets]
  (init-state item)
  (rum/mount (form item volumes secrets) (.getElementById js/document "content")))