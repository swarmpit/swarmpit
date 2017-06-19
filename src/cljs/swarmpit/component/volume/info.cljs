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

(defn- delete-volume-info-msg
  [name]
  (str "Volume " name " has been removed."))

(defn- delete-volume-error-msg
  [error]
  (str "Volume removing failed. Reason: " error))

(defn- delete-volume-handler
  [volume-name]
  (ajax/DELETE (routes/path-for-backend :volume-delete {:name volume-name})
               {:headers       {"Authorization" (storage/get "token")}
                :handler       (fn [_]
                                 (dispatch!
                                   (routes/path-for-frontend :volume-list))
                                 (message/mount!
                                   (delete-volume-info-msg volume-name)))
                :error-handler (fn [{:keys [response]}]
                                 (let [error (get response "error")]
                                   (message/mount!
                                     (delete-volume-error-msg error) true)))}))

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (comp/panel-info icon/volumes
                      (:volumeName item))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-volume-handler (:volumeName item))
          :label      "Delete"}))]]
   [:div.form-view
    [:div.form-view-group
     (comp/form-item "NAME" (:volumeName item))
     (comp/form-item "DRIVER" (:driver item))
     (comp/form-item "SCOPE" (:scope item))
     (comp/form-item "MOUNTPOINT" (:mountpoint item))]]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))