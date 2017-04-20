(ns swarmpit.component.service.info
  (:require [swarmpit.material :as material]
            [swarmpit.router :as router]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-volumes :as volumes]
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.message :as message]
            [rum.core :as rum]
            [ajax.core :refer [DELETE]]))

(enable-console-print!)

(defn- delete-service-handler
  [service-id]
  (DELETE (str "/services/" service-id)
          {:handler       (fn [_]
                            (let [message (str "Service " service-id " has been removed.")]
                              (router/dispatch! "/#/services")
                              (message/mount! message)))
           :error-handler (fn [{:keys [status status-text]}]
                            (let [message (str "Service " service-id " removing failed. Reason: " status-text)]
                              (message/mount! message)))}))

(rum/defc form < rum/static [item]
  (let [id (:id item)]
    [:div
     [:div.form-panel
      [:div.form-panel-right
       (material/theme
         (material/raised-button
           #js {:href    (str "/#/services/" id "/edit")
                :label   "Edit"
                :primary true
                :style   #js {:marginRight "12px"}}))
       (material/theme
         (material/raised-button
           #js {:onTouchTap #(delete-service-handler id)
                :label      "Delete"}))]]
     [:div.form-view
      [:div.form-view-group
       (material/form-view-section "General settings")
       (material/form-view-row "ID" id)
       (material/form-view-row "SERVICE NAME" (:serviceName item))
       (material/form-view-row "CREATED" (:createdAt item))
       (material/form-view-row "LAST UPDATE" (:updatedAt item))
       (material/form-view-row "IMAGE" (:image item))
       (material/form-view-row "IMAGE DIGEST" (:imageDigest item))
       (material/form-view-row "MODE" (:mode item))]
      [:div.form-view-group
       (material/form-view-section "Ports")
       (material/form-view-list ports/form-headers (:ports item) "30%")]
      [:div.form-view-group
       (material/form-view-section "Volumes")
       (material/form-view-list volumes/form-headers (:volumes item) "100%")]
      [:div.form-view-group
       (material/form-view-section "Environment variables")
       (material/form-view-list variables/form-headers (:variables item) "60%")]]]))

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))