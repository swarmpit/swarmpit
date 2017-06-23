(ns swarmpit.component.registry.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- delete-registry-handler
  [registry-id]
  (handler/delete
    (routes/path-for-backend :registry-delete {:id registry-id})
    (fn [_]
      (dispatch!
        (routes/path-for-frontend :registry-list))
      (state/set-value {:text (str "Registry " registry-id " has been removed.")
                        :type :info
                        :open true} message/cursor))
    (fn [response]
      (state/set-value {:text (str "Registry removing failed. Reason: " (:error response))
                        :type :error
                        :open true} message/cursor))))

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (comp/panel-info icon/registries
                      (:name item))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-registry-handler (:_id item))
          :label      "Delete"}))]]
   [:div.form-view
    [:div.form-view-group
     (comp/form-item "ID" (:_id item))
     (comp/form-item "NAME" (:name item))
     (comp/form-item "URL" (:url item))
     (comp/form-item "AUTHENTICATION" (if (:withAuth item)
                                        "yes"
                                        "no"))
     (if (:withAuth item)
       [:div
        (comp/form-item "USERNAME" (:username item))
        (comp/form-item "PASSWORD" (:password item))])]]])
