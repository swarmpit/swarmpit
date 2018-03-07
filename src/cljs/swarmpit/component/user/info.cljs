(ns swarmpit.component.user.info
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.message :as message]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- user-handler
  [user-id]
  (ajax/get
    (routes/path-for-backend :user {:id user-id})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(defn- delete-user-handler
  [user-id]
  (ajax/delete
    (routes/path-for-backend :user-delete {:id user-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :user-list))
                   (message/info
                     (str "User " user-id " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "User removing failed. Reason: " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (user-handler id))))

(rum/defc form-info < rum/static [{:keys [_id] :as item}]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/users
                 (:username item))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:href    (routes/path-for-frontend :user-edit {:id _id})
          :label   "Edit"
          :primary true}))
     [:span.form-panel-delimiter]
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-user-handler _id)
          :disabled   (= (storage/user) (:username item))
          :label      "Delete"}))]]
   [:div.form-view
    [:div.form-view-group
     (form/item "ID" (:_id item))
     (form/item "USERNAME" (:username item))
     (form/item "EMAIL" (:email item))
     (form/item "IS ADMIN" (if (= "admin" (:role item))
                             "yes"
                             "no"))]]])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))