(ns swarmpit.component.secret.info
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [material.component.list-table-auto :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.list :as services]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- secret-services-handler
  [secret-id]
  (ajax/get
    (routes/path-for-backend :secret-services {:id secret-id})
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:services] response state/form-value-cursor))}))

(defn- secret-handler
  [secret-id]
  (ajax/get
    (routes/path-for-backend :secret {:id secret-id})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:secret] response state/form-value-cursor))}))

(defn- delete-secret-handler
  [secret-id]
  (ajax/delete
    (routes/path-for-backend :secret-delete {:id secret-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :secret-list))
                   (message/info
                     (str "Secret " secret-id " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Secret removing failed. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (secret-handler id)
      (secret-services-handler id))))

(rum/defc form-info < rum/static [{:keys [secret services]}]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/secrets
                 (:secretName secret))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-secret-handler (:id secret))
          :label      "Delete"}))]]
   [:div.form-layout
    [:div.form-layout-group
     (form/section "General settings")
     (form/item "ID" (:id secret))
     (form/item "NAME" (:secretName secret))
     (form/item-date "CREATED" (:createdAt secret))
     (form/item-date "UPDATED" (:updatedAt secret))]
    (services/linked-services services)]])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))