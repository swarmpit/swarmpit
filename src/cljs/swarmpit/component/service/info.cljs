(ns swarmpit.component.service.info
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.label :as label]
            [material.component.panel :as panel]
            [material.component.form :as form]
            [material.component.list-table-auto :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.info.settings :as settings]
            [swarmpit.component.service.info.ports :as ports]
            [swarmpit.component.service.info.networks :as networks]
            [swarmpit.component.service.info.mounts :as mounts]
            [swarmpit.component.service.info.secrets :as secrets]
            [swarmpit.component.service.info.configs :as configs]
            [swarmpit.component.service.info.variables :as variables]
            [swarmpit.component.service.info.labels :as labels]
            [swarmpit.component.service.info.logdriver :as logdriver]
            [swarmpit.component.service.info.resources :as resources]
            [swarmpit.component.service.info.deployment :as deployment]
            [swarmpit.component.task.list :as tasks]
            [swarmpit.component.message :as message]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- label [item]
  (str (:state item) "  " (get-in item [:status :info])))

(defn- service-handler
  [service-id]
  (ajax/get
    (routes/path-for-backend :service {:id service-id})
    {:progress   [:loading?]
     :on-success (fn [response]
                   (state/update-value [:service] response state/form-value-cursor))}))

(defn- service-networks-handler
  [service-id]
  (ajax/get
    (routes/path-for-backend :service-networks {:id service-id})
    {:on-success (fn [response]
                   (state/update-value [:networks] response state/form-value-cursor))}))

(defn- service-tasks-handler
  [service-id]
  (ajax/get
    (routes/path-for-backend :service-tasks {:id service-id})
    {:on-success (fn [response]
                   (state/update-value [:tasks] response state/form-value-cursor))}))

(defn- delete-service-handler
  [service-id]
  (ajax/delete
    (routes/path-for-backend :service-delete {:id service-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :service-list))
                   (message/info
                     (str "Service " service-id " has been removed.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Service removing failed. Reason: " (:error response))))}))

(defn- redeploy-service-handler
  [service-id]
  (ajax/post
    (routes/path-for-backend :service-redeploy {:id service-id})
    {:on-success (fn [_]
                   (message/info
                     (str "Service " service-id " redeploy triggered.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Service redeploy failed. Reason: " (:error response))))}))

(defn- rollback-service-handler
  [service-id]
  (ajax/post
    (routes/path-for-backend :service-rollback {:id service-id})
    {:on-success (fn [_]
                   (message/info
                     (str "Service " service-id " rollback triggered.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Service rollback failed. Reason: " (:error response))))}))

(def action-menu-style
  {:position   "relative"
   :marginTop  "13px"
   :marginLeft "66px"})

(def action-menu-item-style
  {:padding "0px 10px 0px 52px"})

(defn- form-action-menu [service-id service-rollback-allowed opened?]
  (comp/mui
    (comp/icon-menu
      {:iconButtonElement (comp/icon-button nil nil)
       :open              opened?
       :style             action-menu-style
       :onRequestChange   (fn [state] (state/update-value [:menu?] state state/form-state-cursor))}
      (comp/menu-item
        {:key           "action-edit"
         :innerDivStyle action-menu-item-style
         :leftIcon      (comp/svg nil icon/edit)
         :onClick       (fn []
                          (dispatch!
                            (routes/path-for-frontend :service-edit {:id service-id})))
         :primaryText   "Edit"})
      (comp/menu-item
        {:key           "action-redeploy"
         :innerDivStyle action-menu-item-style
         :leftIcon      (comp/svg nil icon/redeploy)
         :onClick       #(redeploy-service-handler service-id)
         :primaryText   "Redeploy"})
      (comp/menu-item
        {:key           "action-rollback"
         :innerDivStyle action-menu-item-style
         :leftIcon      (comp/svg nil icon/rollback)
         :onClick       #(rollback-service-handler service-id)
         :disabled      (not service-rollback-allowed)
         :primaryText   "Rollback"})
      (comp/menu-item
        {:key           "action-delete"
         :innerDivStyle action-menu-item-style
         :leftIcon      (comp/svg nil icon/trash)
         :onClick       #(delete-service-handler service-id)
         :primaryText   "Delete"}))))

(rum/defc form-tasks < rum/static [tasks]
  [:div.form-layout-group.form-layout-group-border
   (form/section "Tasks")
   (list/table (map :name tasks/headers)
               (filter #(not (= "shutdown" (:state %))) tasks)
               tasks/render-item
               tasks/render-item-keys
               tasks/onclick-handler)])

(defn- init-form-state
  []
  (state/set-value {:menu?    false
                    :loading? true} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:service  {}
                    :tasks    []
                    :networks []} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (init-form-value)
      (service-handler id)
      (service-networks-handler id)
      (service-tasks-handler id))))

(rum/defc form-info < rum/reactive [{:keys [service networks tasks]}
                                    {:keys [menu?]}]
  (let [ports (:ports service)
        mounts (:mounts service)
        secrets (:secrets service)
        configs (:configs service)
        variables (:variables service)
        labels (:labels service)
        logdriver (:logdriver service)
        resources (:resources service)
        deployment (:deployment service)
        id (:id service)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/services
                   (:serviceName service)
                   (label/info
                     (label service)))]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href  (routes/path-for-frontend :service-log {:id id})
            :icon  (comp/button-icon icon/log-18)
            :label "Logs"}))
       [:span.form-panel-delimiter]
       [:div
        (comp/mui
          (comp/raised-button
            {:onClick       (fn [_] (state/update-value [:menu?] true state/form-state-cursor))
             :icon          (comp/button-icon icon/expand-18)
             :labelPosition "before"
             :label         "Actions"}))
        (form-action-menu id (:rollbackAllowed deployment) menu?)]]]
     [:div.form-layout
      (settings/form service)
      (ports/form ports)
      (networks/form networks)
      (mounts/form mounts)
      (secrets/form secrets)
      (configs/form configs)
      (variables/form variables)
      (labels/form labels)
      (logdriver/form logdriver)
      (resources/form resources)
      (deployment/form deployment)
      (form-tasks tasks)]]))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item state))))