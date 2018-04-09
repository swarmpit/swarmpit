(ns swarmpit.component.dockerhub.info
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.message :as message]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- user-handler
  [user-id]
  (ajax/get
    (routes/path-for-backend :dockerhub-user {:id user-id})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(defn- delete-user-handler
  [user-id]
  (ajax/delete
    (routes/path-for-backend :dockerhub-user-delete {:id user-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :dockerhub-user-list))
                   (message/info
                     (str "User " user-id " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "User removing failed. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (user-handler id))))

(rum/defc form-info < rum/static [{:keys [_id username] :as user}]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/docker username)]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:href    (routes/path-for-frontend :dockerhub-user-edit {:id _id})
          :label   "Edit"
          :primary true}))
     [:span.form-panel-delimiter]
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-user-handler _id)
          :label      "Delete"}))]]
   [:div.form-view
    [:div.form-view-group
     (form/item "ID" (:_id user))
     (form/item "NAME" (:name user))
     (form/item "PUBLIC" (if (:public user)
                           "yes"
                           "no"))
     (form/item "USERNAME" (:username user))
     (form/item "LOCATION" (:location user))
     (form/item "ROLE" (:role user))]]])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        user (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info user))))
