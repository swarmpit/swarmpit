(ns swarmpit.component.secret.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- delete-secret-handler
  [secret-id]
  (handler/delete
    (routes/path-for-backend :secret-delete {:id secret-id})
    (fn [_]
      (dispatch!
        (routes/path-for-frontend :secret-list))
      (state/set-value {:text (str "Secret " secret-id " has been removed.")
                        :type :info
                        :open true} message/cursor))
    (fn [response]
      (state/set-value {:text (str "Secret removing failed. Reason: " (:error response))
                        :type :error
                        :open true} message/cursor))))

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (comp/panel-info icon/secrets
                      (:secretName item))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-secret-handler (:id item))
          :label      "Delete"}))]]
   [:div.form-view
    [:div.form-view-group
     (comp/form-item "ID" (:id item))
     (comp/form-item "NAME" (:secretName item))
     (comp/form-item "CREATED" (:createdAt item))
     (comp/form-item "UPDATED" (:updatedAt item))]]])