(ns swarmpit.component.user.info
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(defonce loading? (atom false))

(defn- user-handler
  [user-id]
  (handler/get
    (routes/path-for-backend :user {:id user-id})
    {:state      loading?
     :on-success (fn [response]
                   (state/set-value response cursor))}))

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

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (user-handler id))))

(rum/defc form-info < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/users
                 (:username item))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:href    (routes/path-for-frontend :user-edit {:id (:_id item)})
          :label   "Edit"
          :primary true}))
     [:span.form-panel-delimiter]
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-user-handler (:_id item))
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
  (let [user (state/react cursor)]
    (progress/form
      (rum/react loading?)
      (form-info user))))