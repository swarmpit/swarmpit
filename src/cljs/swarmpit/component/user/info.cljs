(ns swarmpit.component.user.info
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.label :as label]
            [swarmpit.component.message :as message]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.common :as common]
            [swarmpit.component.progress :as progress]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
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
                     (str "User removing failed. " (:error response))))}))

(defn form-actions
  [username id]
  [{:onClick #(dispatch! (routes/path-for-frontend :user-edit {:id id}))
    :icon    (comp/svg icon/edit-path)
    :name    "Edit user"}
   {:onClick  #(delete-user-handler id)
    :disabled (= (storage/user) username)
    :icon     (comp/svg icon/trash-path)
    :name     "Delete user"}])

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
       [:div.Swarmpit-form-context
        (comp/grid
          {:container true
           :spacing   16}
          (comp/grid
            {:item true
             :key  "ugg"
             :xs   12
             :sm   6}
            (comp/card
              {:className "Swarmpit-form-card"
               :key       "ugc"}
              (comp/card-header
                {:title     username
                 :className "Swarmpit-form-card-header"
                 :key       "ugch"
                 :subheader email
                 :action    (common/actions-menu
                              (form-actions username _id)
                              :userGeneralMenuAnchor
                              :userGeneralMenuOpened)})
              (comp/card-content
                {:key "ugccl"}
                (form/item-labels
                  [(label/grey role)]))
              (comp/divider
                {:key "ugd"})
              (comp/card-content
                {:style {:paddingBottom "16px"}
                 :key   "ugccf"}
                (form/item-id _id)))))]])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))