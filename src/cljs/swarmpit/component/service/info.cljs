(ns swarmpit.component.service.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-networks :as networks]
            [swarmpit.component.service.form-mounts :as mounts]
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.task.list :as tasks]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(defn- delete-service-handler
  [service-id]
  (ajax/DELETE (routes/path-for-backend :service-delete {:id service-id})
               {:headers       {"Authorization" (storage/get "token")}
                :handler       (fn [_]
                                 (let [message (str "Service " service-id " has been removed.")]
                                   (dispatch!
                                     (routes/path-for-frontend :service-list))
                                   (message/mount! message)))
                :error-handler (fn [{:keys [response]}]
                                 (let [error (get response "error")
                                       message (str "Service removing failed. Reason: " error)]
                                   (message/mount! message)))}))

(defn form-panel-label [item]
  (str (:state item) "  " (get-in item [:status :info])))

(defn render-item
  [val]
  (if (boolean? val)
    (comp/checkbox {:checked val})
    val))

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
           {:href    (routes/path-for-frontend :service-edit {:id id})
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
       (comp/form-item "IMAGE" (get-in item [:repository :image]))
       (comp/form-item "IMAGE DIGEST" (get-in item [:repository :imageDigest]))
       (comp/form-item "MODE" (:mode item))]
      [:div.form-view-group
       (comp/form-section "Ports")
       (if (empty? (:ports item))
         ports/undefined
         (comp/info-table ports/headers (:ports item) render-item "300px"))]
      [:div.form-view-group
       (comp/form-section "Networks")
       (if (empty? (:networks item))
         networks/undefined
         (comp/info-table ["Name" "Driver"] (:networks item) render-item "300px"))]
      [:div.form-view-group
       (comp/form-section "Mounts")
       (if (empty? (:mounts item))
         mounts/undefined
         (comp/info-table mounts/headers (:mounts item) render-item "150vh"))]
      [:div.form-view-group
       (comp/form-section "Environment variables")
       (if (empty? (:networks item))
         variables/undefined
         (comp/info-table variables/headers (:variables item) render-item "100vh"))]
      [:div.form-view-group
       (comp/form-section "Tasks")
       (comp/list-table tasks/headers
                        (filter #(not (= "shutdown" (:state %))) (:tasks item))
                        tasks/render-item
                        tasks/render-item-keys
                        :task-info)]]]))

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))