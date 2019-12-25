(ns swarmpit.component.service.info
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.dialog :as dialog]
            [swarmpit.component.service.info.settings :as settings]
            [swarmpit.component.service.info.ports :as ports]
            [swarmpit.component.service.info.networks :as networks]
            [swarmpit.component.service.info.mounts :as mounts]
            [swarmpit.component.service.info.secrets :as secrets]
            [swarmpit.component.service.info.configs :as configs]
            [swarmpit.component.service.info.hosts :as hosts]
            [swarmpit.component.service.info.variables :as variables]
            [swarmpit.component.service.info.labels :as labels]
            [swarmpit.component.service.info.logdriver :as logdriver]
            [swarmpit.component.service.info.resources :as resources]
            [swarmpit.component.service.info.deployment :as deployment]
            [swarmpit.component.service.log :as log]
            [swarmpit.component.toolbar :as toolbar]
            [swarmpit.component.task.list :as tasks]
            [swarmpit.component.message :as message]
            [swarmpit.component.parser :refer [parse-int]]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.docker.utils :as utils]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn- label [item]
  (str (:state item) "  " (get-in item [:status :info])))

(defn- me-handler
  [service-id]
  (ajax/get
    (routes/path-for-backend :me)
    {:on-success (fn [{:keys [response]}]
                   (let [pinned-services (set (:service-dashboard response))]
                     (when (contains? pinned-services service-id)
                       (state/update-value [:pinned?] true state/form-state-cursor))))}))

(defn- stats-handler
  []
  (ajax/get
    (routes/path-for-backend :stats)
    {:state      [:loading? :stats]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:stats] response state/form-value-cursor))
     :on-error   (fn [_])}))

(defn- service-handler
  [service-id]
  (ajax/get
    (routes/path-for-backend :service {:id service-id})
    {:state      [:loading? :service]
     :on-success (fn [{:keys [response]}]
                   (me-handler (:id response))
                   (state/update-value [:service] response state/form-value-cursor))}))

(defn- service-networks-handler
  [service-id]
  (ajax/get
    (routes/path-for-backend :service-networks {:id service-id})
    {:state      [:loading? :networks]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:networks] response state/form-value-cursor))}))

(defn- service-tasks-handler
  [service-id]
  (ajax/get
    (routes/path-for-backend :service-tasks {:id service-id})
    {:state      [:loading? :tasks]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:tasks] response state/form-value-cursor))}))

(defn- delete-service-handler
  [service-id]
  (ajax/delete
    (routes/path-for-backend :service {:id service-id})
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

(defn- pin-service-handler
  [service-id]
  (ajax/post
    (routes/path-for-backend :service-dashboard {:id service-id})
    {:on-success (fn [_]
                   (state/update-value [:pinned?] true state/form-state-cursor)
                   (message/info
                     (str "Service " service-id " pinned to dashboard.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Service pin failed. " (:error response))))}))

(defn- detach-service-handler
  [service-id]
  (ajax/delete
    (routes/path-for-backend :service-dashboard {:id service-id})
    {:on-success (fn [_]
                   (state/update-value [:pinned?] false state/form-state-cursor)
                   (message/info
                     (str "Service " service-id " detached to dashboard.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Service detach failed. " (:error response))))}))

(defn- stop-service-handler
  [service-id]
  (ajax/post
    (routes/path-for-backend :service-stop {:id service-id})
    {:on-success (fn [_]
                   (message/info
                     (str "Service " service-id " is stopping.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Service stopping failed. " (:error response))))}))

(defn form-tasks [service tasks]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     (comp/typography {:variant "h6"} "Tasks")})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (list/responsive
        (list/override-title
          tasks/render-metadata
          #(-> % :id (subs 0 7))
          #(-> % :repository :image (utils/tag)))
        (->> tasks
             (sort-by :createdAt)
             (reverse)
             (filter #(not (= "shutdown" (:state %)))))
        tasks/onclick-handler))))

(defn form-actions
  [service service-id pinned?]
  [(if pinned?
     {:onClick #(detach-service-handler service-id)
      :icon    (comp/svg icon/pin-path)
      :group   true
      :name    "Detach"}
     {:onClick #(pin-service-handler service-id)
      :icon    (comp/svg icon/pin-path)
      :group   true
      :name    "Pin"})
   {:onClick  #(dispatch! (routes/path-for-frontend :service-edit {:id service-id}))
    :icon     (comp/svg icon/edit-path)
    :disabled (true? (:immutable service))
    :main     true
    :group    true
    :name     "Edit"}
   {:onClick #(dispatch! (routes/path-for-frontend :stack-create nil {:from service-id}))
    :icon    (comp/svg icon/stacks-path)
    :group   true
    :name    "Compose"}
   {:onClick #(redeploy-service-handler service-id)
    :icon    (comp/svg icon/redeploy-path)
    :group   true
    :name    "Redeploy"}
   {:onClick  #(rollback-service-handler service-id)
    :disabled (not (get-in service [:deployment :rollbackAllowed]))
    :icon     (comp/svg icon/rollback-path)
    :group    true
    :name     "Rollback"}
   {:onClick  #(stop-service-handler service-id)
    :disabled (or (= "global" (:mode service))
                  (= "not running" (:state service)))
    :icon     (icon/stop {})
    :group    true
    :name     "Stop"}
   {:onClick #(state/update-value [:open] true dialog/dialog-cursor)
    :icon    (comp/svg icon/trash-path)
    :color   "default"
    :variant "outlined"
    :name    "Delete"}])

(defn- init-form-state
  []
  (state/set-value {:cmdAnchor nil
                    :cmdShow   false
                    :pinned?   false
                    :menu?     false
                    :loading?  {:service  true
                                :tasks    true
                                :networks true
                                :stats    true}} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:service  {}
                    :tasks    []
                    :networks []
                    :nodes    []} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (init-form-value)
      (service-handler id)
      (service-networks-handler id)
      (service-tasks-handler id)
      (stats-handler))))

(defn form-settings-grid [service tasks stats]
  (comp/grid
    {:item true
     :xs   12}
    (settings/form service tasks stats)))

(defn form-tasks-grid [service tasks]
  (comp/grid
    {:item true
     :xs   12}
    (form-tasks service tasks)))

(defn form-networks-grid [networks service-id immutable?]
  (comp/grid
    {:item true
     :xs   12}
    (networks/form networks service-id immutable?)))

(defn form-ports-grid [ports service-id immutable?]
  (comp/grid
    {:item true
     :xs   12}
    (ports/form ports service-id immutable?)))

(defn form-mounts-grid [mounts service-id immutable?]
  (comp/grid
    {:item true
     :xs   12}
    (mounts/form mounts service-id immutable?)))

(defn form-secrets-grid [secrets service-id immutable?]
  (comp/grid
    {:item true
     :xs   12}
    (secrets/form secrets service-id immutable?)))

(defn form-configs-grid [configs service-id immutable?]
  (comp/grid
    {:item true
     :xs   12}
    (configs/form configs service-id immutable?)))

(defn form-hosts-grid [hosts service-id immutable?]
  (comp/grid
    {:item true
     :xs   12}
    (hosts/form hosts service-id immutable?)))

(defn form-variables-grid [variables service-id immutable?]
  (comp/grid
    {:item true
     :xs   12}
    (variables/form variables service-id immutable?)))

(defn form-labels-grid [labels service-id immutable?]
  (comp/grid
    {:item true
     :xs   12}
    (labels/form labels service-id immutable?)))

(defn form-logdriver-grid [logdriver service-id immutable?]
  (comp/grid
    {:item true
     :xs   12}
    (logdriver/form logdriver service-id immutable?)))

(defn form-resources-grid [resources service-id immutable?]
  (comp/grid
    {:item true
     :xs   12}
    (resources/form resources service-id immutable?)))

(defn form-deployment-grid [deployment service-id immutable?]
  (comp/grid
    {:item true
     :xs   12}
    (deployment/form deployment service-id immutable?)))

(rum/defc form-info < rum/static [{:keys [service networks tasks stats]}
                                  {:keys [pinned?] :as state}
                                  log]
  (let [ports (:ports service)
        mounts (:mounts service)
        secrets (:secrets service)
        configs (:configs service)
        hosts (:hosts service)
        variables (:variables service)
        labels (:labels service)
        logdriver (:logdriver service)
        resources (:resources service)
        deployment (:deployment service)
        id (:id service)
        immutable? (:immutable service)]
    (comp/mui
      (html
        [:div.Swarmpit-form
         (log/dialog (:serviceName service) nil (= 1 log))
         (dialog/confirm-dialog
           #(delete-service-handler id)
           "Delete service?"
           "Delete")
         [:div.Swarmpit-form-toolbar
          (comp/hidden
            {:xsDown         true
             :implementation "js"}
            (comp/grid
              {:container true
               :spacing   2}
              (comp/grid
                {:item true
                 :xs   12}
                (toolbar/toolbar "Service" (:serviceName service) (form-actions service id pinned?)))
              (comp/grid
                {:item true
                 :sm   6
                 :md   4}
                (comp/grid
                  {:container true
                   :spacing   2}
                  (form-settings-grid service tasks stats)
                  (form-hosts-grid hosts id immutable?)
                  (form-variables-grid variables id immutable?)
                  (form-secrets-grid secrets id immutable?)
                  (form-configs-grid configs id immutable?)
                  (form-logdriver-grid logdriver id immutable?)
                  (form-resources-grid resources id immutable?)
                  (form-labels-grid labels id immutable?)
                  (form-deployment-grid deployment id immutable?)))
              (comp/grid
                {:item true
                 :sm   6
                 :md   8}
                (comp/grid
                  {:container true
                   :spacing   2}
                  (form-tasks-grid service tasks)
                  (form-networks-grid networks id immutable?)
                  (form-ports-grid ports id immutable?)
                  (form-mounts-grid mounts id immutable?)))))
          (comp/hidden
            {:smUp           true
             :implementation "js"}
            (comp/grid
              {:container true
               :spacing   2}
              (comp/grid
                {:item true
                 :xs   12}
                (toolbar/toolbar "Service" (:serviceName service) (form-actions service id pinned?)))
              (form-settings-grid service tasks stats)
              (form-tasks-grid service tasks)
              (form-networks-grid networks id immutable?)
              (form-ports-grid ports id immutable?)
              (form-hosts-grid hosts id immutable?)
              (form-variables-grid variables id immutable?)
              (form-mounts-grid mounts id immutable?)
              (form-secrets-grid secrets id immutable?)
              (form-configs-grid configs id immutable?)
              (form-resources-grid resources id immutable?)
              (form-labels-grid labels id immutable?)
              (form-deployment-grid deployment id immutable?)
              (form-logdriver-grid logdriver id immutable?)))]]))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [{{:keys [log]} :params}]
  (let [{:keys [loading?] :as state} (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (or (:service loading?)
          (:tasks loading?)
          (:networks loading?)
          (:stats loading?))
      (form-info item state (parse-int log)))))