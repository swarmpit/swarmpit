(ns swarmpit.component.config.info
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

(defn- config-services-handler
  [config-id]
  (handler/get
    (routes/path-for-backend :services {} {:filterType  "config"
                                           :filterValue config-id})
    {:on-success (fn [response]
                   (state/update-value [:services] response cursor))}))

(defn- config-handler
  [config-id]
  (handler/get
    (routes/path-for-backend :config {:id config-id})
    {:on-success (fn [response]
                   (state/update-value [:config] response cursor))}))

(defn- delete-config-handler
  [config-id]
  (handler/delete
    (routes/path-for-backend :config-delete {:id config-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :config-list))
                   (message/info
                     (str "Config " config-id " has been removed.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Config removing failed. Reason: " (:error response))))}))

(defn- init-state
  []
  (state/set-value {:config   {}
                    :services []} cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (config-handler id)
      (config-services-handler id))))

(rum/defc form-info < rum/static [config services]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/secrets
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
  (let [{:keys [config services]} (state/react cursor)]
    (progress/form
      (empty? config)
      (form-info config services))))