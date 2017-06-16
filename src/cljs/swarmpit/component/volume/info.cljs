(ns swarmpit.component.volume.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(defn- delete-volume-handler
  [volume-name]
  (ajax/DELETE (routes/path-for-backend :volume-delete {:name volume-name})
               {:headers       {"Authorization" (storage/get "token")}
                :handler       (fn [_]
                                 (let [message (str "Volume " volume-name " has been removed.")]
                                   (dispatch!
                                     (routes/path-for-frontend :volume-list))
                                   (message/mount! message)))
                :error-handler (fn [{:keys [response]}]
                                 (let [error (get response "error")
                                       message (str "Volume removing failed. Reason: " error)]
                                   (message/mount! message)))}))

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (comp/panel-info icon/volumes
                      (:volumeName item))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-volume-handler (:name item))
          :label      "Delete"}))]]
   [:div.form-view
    [:div.form-view-group
     (comp/form-section "General settings")
     (comp/form-item "ID" (:id item))
     (comp/form-item "NAME" (:volumeName item))
     (comp/form-item "DRIVER" (:driver item))
     (comp/form-item "SCOPE" (:scope item))
     (comp/form-item "MOUNTPOINT" (:mountpoint item))]]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))