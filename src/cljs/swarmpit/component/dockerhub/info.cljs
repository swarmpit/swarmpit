(ns swarmpit.component.dockerhub.info
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- delete-user-handler
  [user-id]
  (handler/delete
    (routes/path-for-backend :dockerhub-user-delete {:id user-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :dockerhub-user-list))
                   (message/info
                     (str "User " user-id " has been removed.")))
     :on-error   (fn [response]
                   (message/error
                     (str "User removing failed. Reason: " (:error response))))}))

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/docker
                 (:username item))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:href    (routes/path-for-frontend :dockerhub-user-edit {:id (:_id item)})
          :label   "Edit"
          :primary true}))
     [:span.form-panel-delimiter]
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-user-handler (:_id item))
          :label      "Delete"}))]]
   [:div.form-view
    [:div.form-view-group
     (form/item "ID" (:_id item))
     (form/item "NAME" (:name item))
     (form/item "PUBLIC" (if (:public item)
                                "yes"
                                "no"))
     (form/item "USERNAME" (:username item))
     (form/item "LOCATION" (:location item))
     (form/item "ROLE" (:role item))]]])
