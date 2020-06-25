(ns swarmpit.component.registry-v2.info
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

(defn- registry-handler
  [registry-id]
  (ajax/get
    (routes/path-for-backend :registry {:id           registry-id
                                        :registryType :v2})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(defn- delete-registry-handler
  [registry-id]
  (ajax/delete
    (routes/path-for-backend :registry {:id           registry-id
                                        :registryType :v2})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :registry-list))
                   (message/info
                     (str "Registry " registry-id " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Registry removing failed. " (:error response))))}))

(defn form-actions
  [id]
  [{:onClick #(dispatch! (routes/path-for-frontend :registry-edit {:registryType :v2
                                                                   :id           id}))
    :icon    (comp/svg icon/edit-path)
    :name    "Edit registry"}
   {:onClick #(state/update-value [:open] true dialog/dialog-cursor)
    :icon    (comp/svg icon/trash-path)
    :color   "default"
    :variant "outlined"
    :name    "Delete registry"}])

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (registry-handler id))))

(rum/defc form-info < rum/static [{:keys [_id name url username public withAuth]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       (dialog/confirm-dialog
         #(delete-registry-handler _id)
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
             (toolbar/toolbar "Registry" username (form-actions _id)))
           (comp/grid
             {:item true
              :xs   12}
             (comp/card
               {:className "Swarmpit-form-card"}
               (comp/card-header
                 {:title     (comp/typography {:variant "h6"} "Registry v2")
                  :avatar    (comp/avatar
                               {:className "Swarmpit-card-avatar"}
                               (comp/svg icon/registries-path))
                  :subheader (when public
                               (label/base "Public" "info"))})
               (comp/card-content
                 {}
                 (comp/typography
                   {:variant "body2"}
                   (html [:span "Authenticated with user " [:b username] "."])))
               (form/item-main "ID" _id false)
               (form/item-main "Name" name)
               (form/item-main "Url" url)))))])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        registry (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info registry))))
