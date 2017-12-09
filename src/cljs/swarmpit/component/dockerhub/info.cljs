(ns swarmpit.component.dockerhub.info
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(defn- user-handler
  [user-id]
  (handler/get
    (routes/path-for-backend :dockerhub-user {:id user-id})
    {:on-success (fn [response]
                   (state/set-value response cursor))}))

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

(def mixin-init-state
  (mixin/init-state
    (fn [{:keys [id]}]
      (user-handler id))))

(rum/defc form-info < rum/static [user]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/docker
                 (:username user))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:href    (routes/path-for-frontend :dockerhub-user-edit {:id (:_id user)})
          :label   "Edit"
          :primary true}))
     [:span.form-panel-delimiter]
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-user-handler (:_id user))
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
                 mixin-init-state [_]
  (let [user (state/react cursor)]
    (progress/form
      (nil? user)
      (form-info user))))
