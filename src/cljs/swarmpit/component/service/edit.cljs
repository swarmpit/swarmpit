(ns swarmpit.component.service.edit
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.form-settings :as settings]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-networks :as networks]
            [swarmpit.component.service.form-mounts :as mounts]
            [swarmpit.component.service.form-secrets :as secrets]
            [swarmpit.component.service.form-configs :as configs]
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.service.form-labels :as labels]
            [swarmpit.component.service.form-logdriver :as logdriver]
            [swarmpit.component.service.form-resources :as resources]
            [swarmpit.component.service.form-deployment :as deployment]
            [swarmpit.component.service.form-deployment-placement :as placement]
            [swarmpit.component.message :as message]
            [swarmpit.ajax :as ajax]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn render-item
  [val]
  (if (boolean? val)
    (comp/checkbox {:checked val})
    val))

(defn- service-handler
  [service-id]
  (ajax/get
    (routes/path-for-backend :service {:id service-id})
    {:state [:loading?]
     :on-success
            (fn [{:keys [response]}]
              (settings/tags-handler (-> response :repository :name))
              (state/set-value (select-keys response [:repository :version :serviceName :mode :replicas :stack]) settings/form-value-cursor)
              (state/set-value (:ports response) ports/form-value-cursor)
              (state/set-value (:mounts response) mounts/form-value-cursor)
              (state/set-value (->> (:secrets response)
                                    (map #(select-keys % [:secretName :secretTarget]))
                                    (into [])) secrets/form-value-cursor)
              (state/set-value (->> (:configs response)
                                    (map #(select-keys % [:configName :configTarget]))
                                    (into [])) configs/form-value-cursor)
              (state/set-value (:variables response) variables/form-value-cursor)
              (state/set-value (:labels response) labels/form-value-cursor)
              (state/set-value (:logdriver response) logdriver/form-value-cursor)
              (state/set-value (:resources response) resources/form-value-cursor)
              (state/set-value (:deployment response) deployment/form-value-cursor))}))

(defn- service-networks-handler
  [service-id]
  (ajax/get
    (routes/path-for-backend :service-networks {:id service-id})
    {:on-success
     (fn [{:keys [response]}]
       (state/set-value (->> response
                             (map #(select-keys % [:networkName :serviceAliases]))
                             (into [])) networks/form-value-cursor))}))

(defn- update-service-handler
  [service-id]
  (let [settings (state/get-value settings/form-value-cursor)
        ports (state/get-value ports/form-value-cursor)
        networks (state/get-value networks/form-value-cursor)
        secrets (state/get-value secrets/form-value-cursor)
        configs (state/get-value configs/form-value-cursor)
        variables (state/get-value variables/form-value-cursor)
        labels (state/get-value labels/form-value-cursor)
        logdriver (state/get-value logdriver/form-value-cursor)
        resources (state/get-value resources/form-value-cursor)
        deployment (state/get-value deployment/form-value-cursor)]
    (ajax/post
      (routes/path-for-backend :service-update {:id service-id})
      {:params     (-> settings
                       (assoc :ports ports)
                       (assoc :networks networks)
                       (assoc :mounts (mounts/normalize))
                       (assoc :secrets (when-not (empty? (state/get-value (conj secrets/form-state-cursor :list))) secrets))
                       (assoc :configs (when-not (empty? (state/get-value (conj configs/form-state-cursor :list))) configs))
                       (assoc :variables variables)
                       (assoc :labels labels)
                       (assoc :logdriver logdriver)
                       (assoc :resources resources)
                       (assoc :deployment deployment))
       :state      [:processing?]
       :on-success (fn [{:keys [origin?]}]
                     (when origin?
                       (dispatch!
                         (routes/path-for-frontend :service-info {:id service-id})))
                     (message/info
                       (str "Service " service-id " has been updated.")))
       :on-error   (fn [{:keys [response]}]
                     (message/error
                       (str "Service update failed. " (:error response))))})))

(defn- init-form-state
  []
  (state/set-value {:processing? false
                    :loading?    true} state/form-state-cursor)
  (state/set-value {:valid? true
                    :tags   []} settings/form-state-cursor)
  (state/set-value {:volumes []} mounts/form-state-cursor)
  (state/set-value {:list []} secrets/form-state-cursor)
  (state/set-value {:list []} configs/form-state-cursor)
  (state/set-value {:list []} networks/form-state-cursor)
  (state/set-value {:list []} placement/form-state-cursor)
  (state/set-value {:names []} labels/form-state-cursor)
  (state/set-value {:valid? true} resources/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (service-handler id)
      (service-networks-handler id)
      (mounts/volumes-handler)
      (networks/networks-handler)
      (secrets/secrets-handler)
      (when (<= 1.30 (state/get-value [:docker :api]))
        (configs/configs-handler))
      (placement/placement-handler)
      (labels/labels-handler))))

(rum/defc form-settings < rum/static []
  [:div.form-layout-group
   (form/section "General settings")
   (settings/form true)])

(rum/defc form-ports < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Ports" ports/add-item)
   (ports/form)])

(rum/defc form-networks < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Networks" networks/add-item)
   (networks/form)])

(rum/defc form-mounts < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Mounts" mounts/add-item)
   (mounts/form)])

(rum/defc form-secrets < rum/reactive []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Secrets" secrets/add-item)
   (secrets/form)])

(rum/defc form-configs < rum/reactive []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Configs" configs/add-item)
   (configs/form)])

(rum/defc form-variables < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Environment Variables" variables/add-item)
   (variables/form)])

(rum/defc form-labels < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Labels" labels/add-item)
   (labels/form)])

(rum/defc form-logdriver < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section "Logging")
   (logdriver/form)])

(rum/defc form-resources < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section "Resources")
   (resources/form)])

(rum/defc form-deployment < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section "Deployment")
   (deployment/form)])

(rum/defc form-edit < rum/reactive [id
                                    {:keys [settings]}
                                    {:keys [processing?]}]
  (let [resources-state (state/react resources/form-state-cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/services (:serviceName settings))]
      [:div.form-panel-right
       (comp/progress-button
         {:label      "Save"
          :disabled   (not (:valid? resources-state))
          :primary    true
          :onTouchTap #(update-service-handler id)} processing?)
       [:span.form-panel-delimiter]
       (comp/mui
         (comp/raised-button
           {:href  (routes/path-for-frontend :service-info {:id id})
            :label "Back"}))]]
     [:div.form-layout
      (form-settings)
      (form-ports)
      (form-networks)
      (form-mounts)
      (form-secrets)
      (when (<= 1.30 (state/get-value [:docker :api]))
        (form-configs))
      (form-variables)
      (form-labels)
      (form-logdriver)
      (form-resources)
      (form-deployment)]]))

(rum/defc form < rum/reactive
                 mixin-init-form [{{:keys [id]} :params}]
  (let [state (state/react state/form-state-cursor)
        service (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit id service state))))