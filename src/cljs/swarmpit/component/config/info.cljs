(ns swarmpit.component.config.info
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [material.component.list-table-auto :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.editor :as editor]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.list :as services]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.base64 :as base64]
            [rum.core :as rum]
            [sablono.core :refer-macros [html]]))

(enable-console-print!)

(def editor-id "config-view")

(def form-data-style {:top       "-7px"
                      :maxHeight "400px"})

(defn- parse-data [data]
  (if (base64/base64? data)
    (base64/decode data)
    data))

(defn- form-data [value]
  (html
    (comp/mui
      (comp/text-field
        {:id            editor-id
         :name          "config-view"
         :key           "config-view"
         :multiLine     true
         :value         value
         :style         form-data-style
         :underlineShow false
         :fullWidth     true}))))

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
                     (str "Config removing failed. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (config-handler id)
      (config-services-handler id))))

(def mixin-init-editor
  {:did-mount
   (fn [state]
     (editor/view editor-id)
     state)})

(rum/defc form-info < rum/static
                      mixin-init-editor [{:keys [config services]}]
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
    [:div.div.form-layout-group
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
                 services/onclick-handler)]
    [:div.form-layout-group.form-layout-group-border
     (form/section "Data")
     (form-data (parse-data (:data config)))]]])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))