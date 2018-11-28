(ns swarmpit.component.service.info
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.label :as label]
            [material.component.form :as form]
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
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn- label [item]
  (str (:state item) "  " (get-in item [:status :info])))

(defn- service-handler
  [service-id]
  (ajax/get
    (routes/path-for-backend :service {:id service-id})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:service] response state/form-value-cursor))}))

(defn- service-networks-handler
  [service-id]
  (ajax/get
    (routes/path-for-backend :service-networks {:id service-id})
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:networks] response state/form-value-cursor))}))

(defn- service-tasks-handler
  [service-id]
  (ajax/get
    (routes/path-for-backend :service-tasks {:id service-id})
    {:on-success (fn [{:keys [response]}]
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
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Service removing failed. " (:error response))))}))

(defn- redeploy-service-handler
  [service-id]
  (ajax/post
    (routes/path-for-backend :service-redeploy {:id service-id})
    {:on-success (fn [_]
                   (message/info
                     (str "Service " service-id " redeploy triggered.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Service redeploy failed. " (:error response))))}))

(defn- rollback-service-handler
  [service-id]
  (ajax/post
    (routes/path-for-backend :service-rollback {:id service-id})
    {:on-success (fn [_]
                   (message/info
                     (str "Service " service-id " rollback triggered.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Service rollback failed. " (:error response))))}))

;(rum/defc form-tasks < rum/static [tasks]
;  [:div.form-layout-group.form-layout-group-border
;   (form/subsection "Tasks")
;   (list/table (map :name tasks/headers)
;               (filter #(not (= "shutdown" (:state %))) tasks)
;               tasks/render-item
;               tasks/render-item-keys
;               tasks/onclick-handler)])

(defn form-actions
  [{:keys [params]}]
  [{:button (comp/icon-button
              {:color   "inherit"
               :onClick #(dispatch!
                           (routes/path-for-frontend :service-log {:id (:id params)}))}
              (comp/svg icon/log-18))
    :name   "Service logs"}
   {:button (comp/icon-button
              {:color   "inherit"
               :onClick #(dispatch!
                           (routes/path-for-frontend :service-edit {:id (:id params)}))}
              (comp/svg icon/edit))
    :name   "Edit service"}
   {:button (comp/icon-button
              {:color   "inherit"
               :onClick #(dispatch!
                           (routes/path-for-frontend :stack-create nil {:from (:id params)}))}
              (comp/svg icon/stacks))
    :name   "Compose stack"}
   {:button (comp/icon-button
              {:color   "inherit"
               :onClick #(redeploy-service-handler (:id params))}
              (comp/svg icon/redeploy))
    :name   "Redeploy service"}
   {:button (comp/icon-button
              {:color   "inherit"
               :onClick #(rollback-service-handler (:id params))}
              (comp/svg icon/rollback))
    :name   "Rollback service"}
   {:button (comp/icon-button
              {:color   "inherit"
               :onClick #(delete-service-handler (:id params))}
              (comp/svg icon/trash))
    :name   "Delete service"}])

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

(rum/defc form-info < rum/reactive [{:keys [service networks tasks]}]
  (let [ports (:ports service)
        mounts (:mounts service)
        secrets (:secrets service)
        configs (:configs service)
        variables (:variables service)
        labels (:labels service)
        logdriver (:logdriver service)
        resources (:resources service)
        deployment (:deployment service)
        id (:id service)
        is-even-and-not-third? #(and (even? %) (not (= 2 %)))]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (comp/masonry-like-grid
            {:first-col-pred is-even-and-not-third?}
            (settings/form service)
            (deployment/form deployment)
            (when (not-empty networks)
              (networks/form networks))
            (when (not-empty mounts)
              (mounts/form mounts))
            (when (not-empty secrets)
              (secrets/form secrets))
            (when (not-empty configs)
              (configs/form configs))
            (when (not-empty variables)
              (variables/form variables))
            (when (not-empty labels)
              (labels/form labels))
            (when (not-empty (:opts logdriver))
              (logdriver/form logdriver)))]]))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))
