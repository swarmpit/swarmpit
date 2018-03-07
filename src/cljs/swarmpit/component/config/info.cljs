(ns swarmpit.component.config.info
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

(defn- config-services-handler
  [config-id]
  (ajax/get
    (routes/path-for-backend :config-services {:id config-id})
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:services] response state/form-value-cursor))}))

(defn- config-handler
  [config-id]
  (ajax/get
    (routes/path-for-backend :config {:id config-id})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:config] response state/form-value-cursor))}))

(defn- delete-config-handler
  [config-id]
  (ajax/delete
    (routes/path-for-backend :config-delete {:id config-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :config-list))
                   (message/info
                     (str "Config " config-id " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Config removing failed. Reason: " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (config-handler id)
      (config-services-handler id))))

(rum/defc form-info < rum/static [{:keys [config services]}]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/configs
                 (:configName config))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-config-handler (:id config))
          :label      "Delete"}))]]
   [:div.form-layout
    [:div.form-layout-group
     (form/section "General settings")
     (form/item "ID" (:id config))
     (form/item "NAME" (:configName config))
     (form/item-date "CREATED" (:createdAt config))
     (form/item-date "UPDATED" (:updatedAt config))]
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
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))