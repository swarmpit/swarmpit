(ns swarmpit.component.user.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- delete-user-handler
  [user-id]
  (handler/delete
    (routes/path-for-backend :user-delete {:id user-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :user-list))
                   (message/info
                     (str "User " user-id " has been removed.")))
     :on-error   (fn [response]
                   (message/error
                     (str "User removing failed. Reason: " (:error response))))}))

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
          :disabled   (= (storage/user) (:username item))
          :label      "Delete"}))]]
   [:div.form-view
    [:div.form-view-group
     (comp/form-item "ID" (:_id item))
     (comp/form-item "USERNAME" (:username item))
     (comp/form-item "EMAIL" (:email item))
     (comp/form-item "IS ADMIN" (if (= "admin" (:role item))
                                  "yes"
                                  "no"))]]])