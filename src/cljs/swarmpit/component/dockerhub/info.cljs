(ns swarmpit.component.dockerhub.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(defn- delete-user-handler
  [user-id]
  (ajax/DELETE (routes/path-for-backend :dockerhub-user-delete {:id user-id})
               {:headers       {"Authorization" (storage/get "token")}
                :handler       (fn [_]
                                 (let [message (str "User " user-id " has been removed.")]
                                   (dispatch!
                                     (routes/path-for-frontend :dockerhub-user-list))
                                   (message/mount! message)))
                :error-handler (fn [{:keys [response]}]
                                 (let [error (get response "error")
                                       message (str "User removing failed. Reason: " error)]
                                   (message/mount! message)))}))

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (comp/panel-info icon/docker
                      (:name item))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-user-handler (:_id item))
          :label      "Delete"}))]]
   [:div.form-view
    [:div.form-view-group
     (comp/form-item "ID" (:_id item))
     (comp/form-item "NAME" (:name item))
     (comp/form-item "USERNAME" (:username item))
     (comp/form-item "LOCATION" (:location item))
     (comp/form-item "ROLE" (:role item))]]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))
