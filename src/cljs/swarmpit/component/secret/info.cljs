(ns swarmpit.component.secret.info
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

(defn- delete-secret-handler
  [secret-id]
  (handler/delete
    (routes/path-for-backend :secret-delete {:id secret-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :secret-list))
                   (message/info
                     (str "Secret " secret-id " has been removed.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Secret removing failed. Reason: " (:error response))))}))

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/secrets
                 (:secretName item))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-secret-handler (:id item))
          :label      "Delete"}))]]
   [:div.form-view
    [:div.form-view-group
     (form/item "ID" (:id item))
     (form/item "NAME" (:secretName item))
     (form/item-date "CREATED" (:createdAt item))
     (form/item-date "UPDATED" (:updatedAt item))]]])