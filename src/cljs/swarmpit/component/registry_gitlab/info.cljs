(ns swarmpit.component.registry-gitlab.info
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

(defn- registry-handler
  [registry-id]
  (ajax/get
    (routes/path-for-backend :registry {:id           registry-id
                                        :registryType :gitlab})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(defn- delete-registry-handler
  [registry-id]
  (ajax/delete
    (routes/path-for-backend :registry {:id           registry-id
                                        :registryType :gitlab})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :registry-list))
                   (message/info
                     (str "Gitlab registry " registry-id " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Gitlab registry removing failed. " (:error response))))}))

(defn form-actions
  [id]
  [{:onClick #(dispatch! (routes/path-for-frontend :registry-edit {:registryType :gitlab
                                                                   :id           id}))
    :icon    (comp/svg icon/edit-path)
    :name    "Edit registry"}
   {:onClick #(state/update-value [:open] true dialog/dialog-cursor)
    :icon    (comp/svg icon/trash-path)
    :name    "Delete registry"}])

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (registry-handler id))))

(rum/defc form-info < rum/static [{:keys [_id username url public]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       (dialog/confirm-dialog
         #(delete-registry-handler _id)
         "Remove account?"
         "Remove")
       [:div.Swarmpit-form-context
        (comp/card
          {:className "Swarmpit-form-card Swarmpit-form-card-single"}
          (comp/card-header
            {:title     username
             :className "Swarmpit-form-card-header Swarmpit-card-header-responsive-title"
             :subheader url
             :action    (menu/menu
                          (form-actions _id)
                          :gitlabGeneralMenuAnchor
                          :gitlabGeneralMenuOpened)})
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
              [(label/grey "Gitlab registry")]))
          (comp/divider
            {})
          (comp/card-content
            {:style {:paddingBottom "16px"}}
            (form/item-id _id)))]])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        registry (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info registry))))
