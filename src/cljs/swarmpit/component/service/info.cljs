(ns swarmpit.component.service.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.service.info.settings :as settings]
            [swarmpit.component.service.info.ports :as ports]
            [swarmpit.component.service.info.networks :as networks]
            [swarmpit.component.service.info.mounts :as mounts]
            [swarmpit.component.service.info.secrets :as secrets]
            [swarmpit.component.service.info.variables :as variables]
            [swarmpit.component.service.info.labels :as labels]
            [swarmpit.component.service.info.logdriver :as logdriver]
            [swarmpit.component.service.info.deployment :as deployment]
            [swarmpit.component.task.list :as tasks]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defonce action-menu (atom false))

(defonce service-tasks (atom nil))

(defonce service-networks (atom nil))

(defn- form-panel-label [item]
  (str (:state item) "  " (get-in item [:status :info])))

(defn- delete-service-handler
  [service-id]
  (handler/delete
    (routes/path-for-backend :service-delete {:id service-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :service-list))
                   (message/info
                     (str "Service " service-id " has been removed.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Service removing failed. Reason: " (:error response))))}))

(def form-action-menu-style
  {:position   "relative"
   :marginTop  "13px"
   :marginLeft "66px"})

(defn- form-action-menu [service-id opened?]
  (print opened?)
  (comp/mui
    (comp/icon-menu
      {:iconButtonElement (comp/icon-button nil nil)
       :open              opened?
       :style             form-action-menu-style
       :onRequestChange   (fn [state] (reset! action-menu state))}
      (comp/menu-item
        {:key         "action-edit"
         :leftIcon    (comp/svg nil icon/edit)
         :onClick     (fn []
                        (dispatch!
                          (routes/path-for-frontend :service-edit {:id service-id})))
         :primaryText "Edit"})
      (comp/menu-item
        {:key         "action-redeploy"
         :leftIcon    (comp/svg nil icon/redeploy)
         :primaryText "Redeploy"})
      (comp/menu-item
        {:key         "action-delete"
         :leftIcon    (comp/svg nil icon/trash)
         :onClick     (fn []
                        (delete-service-handler service-id))
         :primaryText "Delete"}))))

(rum/defc form-tasks < rum/static [tasks]
  [:div.form-service-view-group.form-service-group-border
   (comp/form-section "Tasks")
   (comp/list-table-auto ["Name" "Service" "Image" "Node" "Status"]
                         (filter #(not (= "shutdown" (:state %))) tasks)
                         tasks/render-item
                         tasks/render-item-keys
                         tasks/onclick-handler)])

(rum/defc form < rum/reactive
                 rum/static [data]
  (let [opened? (rum/react action-menu)
        {:keys [service networks tasks]} data
        ports (:ports service)
        mounts (:mounts service)
        secrets (:secrets service)
        variables (:variables service)
        labels (:labels service)
        logdriver (:logdriver service)
        deployment (:deployment service)
        id (:id service)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/services
                        (:serviceName service)
                        (comp/label-info
                          (form-panel-label service)))]
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
            {:onClick       (fn [_] (reset! action-menu true))
             :icon          (comp/button-icon icon/expand-18)
             :labelPosition "before"
             :label         "Actions"}))
        (form-action-menu id opened?)]]]
     [:div.form-service-view
      (settings/form service)
      (ports/form ports)
      (networks/form networks)
      (mounts/form mounts)
      (secrets/form secrets)
      (variables/form variables)
      (labels/form labels)
      (logdriver/form logdriver)
      (deployment/form deployment)
      (form-tasks tasks)]]))