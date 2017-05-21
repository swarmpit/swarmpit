(ns swarmpit.component.service.info
  (:require [material.component :as comp]
            [swarmpit.uri :refer [dispatch!]]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-volumes :as volumes]
            [swarmpit.component.service.form-variables :as variables]
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

(rum/defc form < rum/static [item]
  (let [id (:id item)]
    [:div
     [:div.form-panel
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (str "/#/services/" id "/edit")
            :label   "Edit"
            :primary true
            :style   {:marginRight "12px"}}))
       (comp/mui
         (comp/raised-button
           {:onTouchTap #(delete-service-handler id)
            :label      "Delete"}))]]
     [:div.form-view
      [:div.form-view-group
       (comp/form-view-section "General settings")
       (comp/form-view-row "ID" id)
       (comp/form-view-row "SERVICE NAME" (:serviceName item))
       (comp/form-view-row "CREATED" (:createdAt item))
       (comp/form-view-row "LAST UPDATE" (:updatedAt item))
       (comp/form-view-row "IMAGE" (:image item))
       (comp/form-view-row "IMAGE DIGEST" (:imageDigest item))
       (comp/form-view-row "MODE" (:mode item))]
      [:div.form-view-group
       (comp/form-view-section "Ports")
       (comp/form-view-list ports/form-headers (:ports item) "30%")]
      [:div.form-view-group
       (comp/form-view-section "Volumes")
       (comp/form-view-list volumes/form-headers (:volumes item) "100%")]
      [:div.form-view-group
       (comp/form-view-section "Environment variables")
       (comp/form-view-list variables/form-headers (:variables item) "60%")]]]))

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))