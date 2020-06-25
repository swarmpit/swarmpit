(ns swarmpit.component.registry-ecr.info
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

(defn- ecr-handler
  [ecr-id]
  (ajax/get
    (routes/path-for-backend :registry {:id           ecr-id
                                        :registryType :ecr})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(defn- delete-ecr-handler
  [ecr-id]
  (ajax/delete
    (routes/path-for-backend :registry {:id           ecr-id
                                        :registryType :ecr})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :registry-list))
                   (message/info
                     (str "Registry " ecr-id " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Registry removing failed. " (:error response))))}))

(defn form-actions
  [id]
  [{:onClick #(dispatch! (routes/path-for-frontend :registry-edit {:registryType :ecr
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
      (ecr-handler id))))

(rum/defc form-info < rum/static [{:keys [_id url user public]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       (dialog/confirm-dialog
         #(delete-ecr-handler _id)
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
             (toolbar/toolbar "Registry" user (form-actions _id)))
           (comp/grid
             {:item true
              :xs   12}
             (comp/card
               {:className "Swarmpit-form-card"}
               (comp/card-header
                 {:title     (comp/typography {:variant "h6"} "Amazon ECR")
                  :avatar    (comp/avatar
                               {:className "Swarmpit-card-avatar"}
                               (comp/svg icon/amazon-path))
                  :subheader (when public
                               (label/base "Public" "info"))})
               (comp/card-content
                 {}
                 (comp/typography
                   {:variant "body2"}
                   (html [:span "Authenticated with IAM user " [:b user] "."])))
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
