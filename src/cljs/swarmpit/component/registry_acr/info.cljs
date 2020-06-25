(ns swarmpit.component.registry-acr.info
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.label :as label]
            [swarmpit.component.message :as message]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.dialog :as dialog]
            [swarmpit.component.toolbar :as toolbar]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn- acr-handler
  [acr-id]
  (ajax/get
    (routes/path-for-backend :registry {:id           acr-id
                                        :registryType :acr})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(defn- delete-acr-handler
  [acr-id]
  (ajax/delete
    (routes/path-for-backend :registry {:id           acr-id
                                        :registryType :acr})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :registry-list))
                   (message/info
                     (str "Registry " acr-id " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Registry removing failed. " (:error response))))}))

(defn form-actions
  [id]
  [{:onClick #(dispatch! (routes/path-for-frontend :registry-edit {:registryType :acr
                                                                   :id           id}))
    :icon    (comp/svg icon/edit-path)
    :name    "Edit"}
   {:onClick #(state/update-value [:open] true dialog/dialog-cursor)
    :icon    (comp/svg icon/trash-path)
    :color   "default"
    :variant "outlined"
    :name    "Delete"}])

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (acr-handler id))))

(rum/defc form-info < rum/static [{:keys [_id url spName public]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       (dialog/confirm-dialog
         #(delete-acr-handler _id)
         "Remove account?"
         "Remove")
       (comp/container
         {:maxWidth  "md"
          :className "Swarmpit-container"}
         (comp/grid
           {:container true
            :spacing   2}
           (comp/grid
             {:item true
              :xs   12}
             (toolbar/toolbar "Registry" spName (form-actions _id)))
           (comp/grid
             {:item true
              :xs   12}
             (comp/card
               {:className "Swarmpit-form-card"}
               (comp/card-header
                 {:title     (comp/typography {:variant "h6"} "Azure ACR")
                  :avatar    (comp/avatar
                               {:className "Swarmpit-card-avatar"}
                               (comp/svg icon/azure-path))
                  :subheader (when public
                               (label/base "Public" "info"))})
               (comp/card-content
                 {}
                 (comp/typography
                   {:variant "body2"}
                   (html [:span "Authenticated with service principal " [:b spName] "."])))
               (form/item-main "ID" _id false)
               (form/item-main "Url" url)))))])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        registry (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info registry))))
