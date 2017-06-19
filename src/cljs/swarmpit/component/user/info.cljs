(ns swarmpit.component.user.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(defn- delete-user-info-msg
  [id]
  (str "User " id " has been removed."))

(defn- delete-user-error-msg
  [error]
  (str "User removing failed. Reason: " error))

(defn- delete-user-handler
  [user-id]
  (ajax/DELETE (routes/path-for-backend :user-delete {:id user-id})
               {:headers       {"Authorization" (storage/get "token")}
                :handler       (fn [_]
                                 (dispatch!
                                   (routes/path-for-frontend :user-list))
                                 (message/mount!
                                   (delete-user-info-msg user-id)))
                :error-handler (fn [{:keys [response]}]
                                 (let [error (get response "error")]
                                   (message/mount!
                                     (delete-user-error-msg error) true)))}))

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (comp/panel-info icon/users
                      (:username item))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-user-handler (:_id item))
          :label      "Delete"}))]]
   [:div.form-view
    [:div.form-view-group
     (comp/form-item "ID" (:_id item))
     (comp/form-item "USERNAME" (:username item))
     (comp/form-item "EMAIL" (:email item))
     (comp/form-item "IS ADMIN" (if (= "admin" (:role item))
                                  "yes"
                                  "no"))]]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))
