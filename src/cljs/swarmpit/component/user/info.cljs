(ns swarmpit.component.user.info
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.label :as label]
            [swarmpit.component.message :as message]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.common :as common]
            [swarmpit.component.dialog :as dialog]
            [swarmpit.component.toolbar :as toolbar]
            [swarmpit.component.progress :as progress]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [clojure.string :as str]))

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
    (routes/path-for-backend :user {:id user-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :user-list))
                   (message/info
                     (str "User " user-id " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "User removing failed. " (:error response))))}))

(defn form-actions
  [username id]
  [{:onClick #(dispatch! (routes/path-for-frontend :user-edit {:id id}))
    :icon    (comp/svg icon/edit-path)
    :name    "Edit"}
   {:onClick  #(state/update-value [:open] true dialog/dialog-cursor)
    :disabled (= (storage/user) username)
    :icon     (comp/svg icon/trash-path)
    :color    "default"
    :variant  "outlined"
    :name     "Delete"}])

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (user-handler id))))

(rum/defc form-info < rum/static [{:keys [_id username email role]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       (dialog/confirm-dialog
         #(delete-user-handler _id)
         "Delete user?"
         "Delete")
       (comp/container
         {:maxWidth  "md"
          :className "Swarmpit-container"}
         (comp/grid
           {:container true
            :spacing   2}
           (comp/grid
             {:item true
              :xs   12}
             (toolbar/toolbar "User" username (form-actions username _id)))
           (comp/grid
             {:item true
              :xs   12}
             (comp/card
               {:className "Swarmpit-form-card"}
               (form/item-main "ID" _id false)
               (form/item-main "Email" (if (str/blank? email) "-" email))
               (form/item-main "Role" role)))))])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))