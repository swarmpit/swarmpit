(ns swarmpit.component.secret.info
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [material.component.list-table-auto :as list]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.list :as services]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(defn- secret-services-handler
  [secret-id]
  (handler/get
    (routes/path-for-backend :secret-services {:id secret-id})
    {:on-success (fn [response]
                   (state/update-value [:services] response cursor))}))

(defn- secret-handler
  [secret-id]
  (handler/get
    (routes/path-for-backend :secret {:id secret-id})
    {:on-success (fn [response]
                   (state/update-value [:secret] response cursor))}))

(defn- delete-secret-handler
  [secret-id]
  (handler/delete
    (routes/path-for-backend :secret-delete {:id secret-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :secret-list))
                   (message/info
                     (str "Secret " secret-id " has been removed.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Secret removing failed. Reason: " (:error response))))}))

(defn- init-state
  []
  (state/set-value {:secret   {}
                    :services []} cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (secret-handler id)
      (secret-services-handler id))))

(rum/defc form-info < rum/static [secret services]
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
    [:div.form-layout-group.form-layout-group-border
     (form/section "Linked Services")
     (list/table (map :name services/headers)
                 services
                 services/render-item
                 services/render-item-keys
                 services/onclick-handler)]]])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [{:keys [secret services]} (state/react cursor)]
    (progress/form
      (empty? secret)
      (form-info secret services))))