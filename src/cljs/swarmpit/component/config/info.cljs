(ns swarmpit.component.config.info
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
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
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def editor-id "config-view")

(defn- parse-data [data]
  (if (base64/base64? data)
    (base64/decode data)
    data))

(defn- form-data [value]
  (comp/text-field
    {:id              editor-id
     :className       "Swarmpit-codemirror-view"
     :fullWidth       true
     :name            "config-view"
     :key             "config-view"
     :multiline       true
     :disabled        true
     :required        true
     :InputLabelProps {:shrink true}
     :value           value}))

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

(defn form-actions
  [{:keys [params]}]
  [{:onClick #(delete-config-handler (:id params))
    :icon    (comp/svg icon/trash)
    :name    "Delete config"}])

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
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (comp/grid
          {:container true
           :spacing   40}
          (comp/grid
            {:item true
             :key  "cgg"
             :xs   12
             :sm   6}
            (comp/card
              {:className "Swarmpit-form-card"
               :key       "cgc"}
              (comp/card-header
                {:title     (:configName config)
                 :className "Swarmpit-form-card-header"
                 :key       "cgch"})
              (comp/card-content
                {:key "cgcc"}
                (form-data (parse-data (:data config))))
              (comp/divider
                {:key "cgd"})
              (comp/card-content
                {:style {:paddingBottom "16px"}
                 :key   "cgccf"}
                (form/item-date (:createdAt config) (:updatedAt config))
                (form/item-id (:id config)))))
          (when (not-empty services)
            (comp/grid
              {:item true
               :key  "clsg"
               :xs   12}
              (services/linked services))))]])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))