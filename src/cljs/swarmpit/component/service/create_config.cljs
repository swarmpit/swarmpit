(ns swarmpit.component.service.create-config
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.state :as state]
            [swarmpit.component.service.form-settings :as settings]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-networks :as networks]
            [swarmpit.component.service.form-mounts :as mounts]
            [swarmpit.component.service.form-secrets :as secrets]
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.service.form-labels :as labels]
            [swarmpit.component.service.form-logdriver :as logdriver]
            [swarmpit.component.service.form-resources :as resources]
            [swarmpit.component.service.form-deployment :as deployment]
            [swarmpit.component.service.form-deployment-placement :as placement]
            [swarmpit.component.message :as message]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- create-service-handler
  []
  (let [settings (state/get-value settings/cursor)
        ports (state/get-value ports/cursor)
        networks (state/get-value networks/cursor)
        secrets (state/get-value secrets/cursor)
        variables (state/get-value variables/cursor)
        labels (state/get-value labels/cursor)
        logdriver (state/get-value logdriver/cursor)
        resources (state/get-value resources/cursor)
        deployment (state/get-value deployment/cursor)]
    (handler/post
      (routes/path-for-backend :service-create)
      {:params     (-> settings
                       (assoc :ports ports)
                       (assoc :networks networks)
                       (assoc :mounts (mounts/normalize))
                       (assoc :secrets secrets)
                       (assoc :variables variables)
                       (assoc :labels labels)
                       (assoc :logdriver logdriver)
                       (assoc :resources resources)
                       (assoc :deployment deployment))
       :on-success (fn [response]
                     (dispatch!
                       (routes/path-for-frontend :service-info (select-keys response [:id])))
                     (message/info
                       (str "Service " (:id response) " has been created.")))
       :on-error   (fn [response]
                     (message/error
                       (str "Service creation failed. Reason: " (:error response))))})))

(defn init-state
  [distribution distributionType repository]
  (state/set-value {:distribution {:id   distribution
                                   :type distributionType}
                    :repository   {:name repository
                                   :tag  ""
                                   :tags []}
                    :serviceName  ""
                    :mode         "replicated"
                    :replicas     1} settings/cursor)
  (state/set-value [] ports/cursor)
  (state/set-value [] networks/cursor)
  (state/set-value [] mounts/cursor)
  (state/set-value [] secrets/cursor)
  (state/set-value [] variables/cursor)
  (state/set-value [] labels/cursor)
  (state/set-value {:name "json-file"
                    :opts []} logdriver/cursor)
  (state/set-value {:autoredeploy  false
                    :restartPolicy {:condition "any"
                                    :delay     5
                                    :attempts  0}
                    :update        {:parallelism   1
                                    :delay         0
                                    :failureAction "pause"}
                    :rollback      {:parallelism   1
                                    :delay         0
                                    :failureAction "pause"}} deployment/cursor)
  (state/set-value {:reservation {:cpu    0.000
                                  :memory 0}
                    :limit       {:cpu    0.000
                                  :memory 0}} resources/cursor)
  (state/set-value [] placement/cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [repository distribution distributionType]} :params}]
      (init-state distribution
                  distributionType
                  repository)
      (mounts/volumes-handler)
      (networks/networks-handler)
      (secrets/secrets-handler)
      (placement/placement-handler)
      (labels/labels-handler)
      (settings/tags-handler distributionType distribution repository))))

(rum/defc form-settings < rum/static []
  [:div.form-layout-group
   (form/section "General settings")
   (settings/form false)])

(rum/defc form-ports < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Ports" ports/add-item)
   (ports/form-create)])

(rum/defc form-networks < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Networks" networks/add-item)
   (networks/form-create)])

(rum/defc form-mounts < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Mounts" mounts/add-item)
   (mounts/form-create)])

(rum/defc form-secrets < rum/reactive []
  [:div.form-layout-group.form-layout-group-border
   (if (empty? (rum/react secrets/secrets-list))
     (form/section "Secrets")
     (form/section-add "Secrets" secrets/add-item))
   (secrets/form-create)])

(rum/defc form-variables < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Environment Variables" variables/add-item)
   (variables/form-create)])

(rum/defc form-labels < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Labels" labels/add-item)
   (labels/form-create)])

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

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/services "New service")]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:label      "Create"
          :disabled   (or (not (rum/react settings/isValid))
                          (not (rum/react resources/isValid)))
          :primary    true
          :onTouchTap create-service-handler}))]]
   [:div.form-layout
    (form-settings)
    (form-ports)
    (form-networks)
    (form-mounts)
    (form-secrets)
    (form-variables)
    (form-labels)
    (form-logdriver)
    (form-resources)
    (form-deployment)]])