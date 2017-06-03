(ns swarmpit.component.service.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-volumes :as volumes]
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.task.list :as tasks]
            [swarmpit.component.message :as message]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(defn- delete-service-handler
  [service-id]
  (ajax/DELETE (str "/services/" service-id)
               {:handler       (fn [_]
                                 (let [message (str "Service " service-id " has been removed.")]
                                   (dispatch! "/#/services")
                                   (message/mount! message)))
                :error-handler (fn [{:keys [status status-text]}]
                                 (let [message (str "Service " service-id " removing failed. Reason: " status-text)]
                                   (message/mount! message)))}))

(defn form-panel-label [item]
  (str (:state item) "  " (get-in item [:status :info])))

(rum/defc form < rum/static [item]
  (let [id (:id item)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/services
                        (:serviceName item)
                        (comp/label-info
                          (form-panel-label item)))]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (str "/#/services/" id "/edit")
            :label   "Edit"
            :primary true}))
       [:span.form-panel-delimiter]
       (comp/mui
         (comp/raised-button
           {:onTouchTap #(delete-service-handler id)
            :label      "Delete"}))]]
     [:div.form-view
      [:div.form-view-group
       (comp/form-section "General settings")
       (comp/form-item "ID" id)
       (comp/form-item "SERVICE NAME" (:serviceName item))
       (comp/form-item "CREATED" (:createdAt item))
       (comp/form-item "LAST UPDATE" (:updatedAt item))
       (comp/form-item "IMAGE" (:image item))
       (comp/form-item "IMAGE DIGEST" (:imageDigest item))
       (comp/form-item "MODE" (:mode item))]
      [:div.form-view-group
       (comp/form-section "Ports")
       (comp/info-table ports/headers (:ports item) "30%")]
      [:div.form-view-group
       (comp/form-section "Volumes")
       (comp/info-table volumes/headers (:volumes item) "100%")]
      [:div.form-view-group
       (comp/form-section "Environment variables")
       (comp/info-table variables/headers (:variables item) "60%")]
      [:div.form-view-group
       (comp/form-section "Tasks")
       (comp/list-table tasks/headers
                        (filter #(not (= "shutdown" (:state %))) (:tasks item))
                        tasks/render-item
                        tasks/render-item-keys
                        "/#/tasks/")]]]))

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))