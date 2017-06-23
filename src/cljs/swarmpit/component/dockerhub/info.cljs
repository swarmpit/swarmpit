(ns swarmpit.component.dockerhub.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- delete-user-handler
  [user-id]
  (handler/delete
    (routes/path-for-backend :dockerhub-user-delete {:id user-id})
    (fn [_]
      (dispatch!
        (routes/path-for-frontend :dockerhub-user-list))
      (state/set-value {:text (str "User " user-id " has been removed.")
                        :type :info
                        :open true} message/cursor))
    (fn [response]
      (state/set-value {:text (str "User removing failed. Reason: " (:error response))
                        :type :error
                        :open true} message/cursor))))

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (comp/panel-info icon/docker
                      (:username item))]
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
