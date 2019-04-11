(ns swarmpit.component.registry-dockerhub.info
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.label :as label]
            [swarmpit.component.message :as message]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.dialog :as dialog]
            [swarmpit.component.action-menu :as menu]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn- user-handler
  [user-id]
  (ajax/get
    (routes/path-for-backend :registry {:id           user-id
                                        :registryType :dockerhub})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(defn- delete-user-handler
  [user-id]
  (ajax/delete
    (routes/path-for-backend :registry-delete {:id           user-id
                                               :registryType :dockerhub})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :registry-list))
                   (message/info
                     (str "Dockerhub account " user-id " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Dockerhub account removal failed. " (:error response))))}))

(defn form-actions
  [id]
  [{:onClick #(dispatch! (routes/path-for-frontend :registry-edit {:registryType :dockerhub
                                                                   :id           id}))
    :icon    (comp/svg icon/edit-path)
    :name    "Edit account"}
   {:onClick #(state/update-value [:open] true dialog/dialog-cursor)
    :icon    (comp/svg icon/trash-path)
    :name    "Delete account"}])

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (user-handler id))))

(rum/defc form-info < rum/static [{:keys [_id username role public]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       (dialog/confirm-dialog
         #(delete-user-handler _id)
         "Remove account?"
         "Remove")
       [:div.Swarmpit-form-context
        (comp/card
          {:className "Swarmpit-form-card Swarmpit-form-card-single"}
          (comp/card-header
            {:title     username
             :className "Swarmpit-form-card-header Swarmpit-card-header-responsive-title"
             :action    (menu/menu
                          (form-actions _id)
                          :dockerhubMenuAnchor
                          :dockerhubMenuOpened)})
          (comp/card-content
            {}
            (html
              [:div
               [:span "Authenticated with user " [:b username] "."]
               [:br]
               [:span "Account is " [:b (if public "public." "private.")]]]))
          (comp/card-content
            {}
            (form/item-labels
              [(label/grey "dockerhub")]))
          (comp/divider
            {})
          (comp/card-content
            {:style {:paddingBottom "16px"}}
            (form/item-id _id)))]])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        user (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info user))))
