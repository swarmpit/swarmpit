(ns swarmpit.component.config.info
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.dialog :as dialog]
            [swarmpit.component.editor :as editor]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.list :as services]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.base64 :as base64]
            [sablono.core :refer-macros [html]]
            [clojure.contrib.inflect :as inflect]
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
     :className       "Swarmpit-codemirror-info"
     :fullWidth       true
     :name            "config-view"
     :key             "config-view"
     :multiline       true
     :disabled        true
     :required        true
     :InputProps      {:style {:padding 0}}
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
    (routes/path-for-backend :config {:id config-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :config-list))
                   (message/info
                     (str "Config " config-id " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Config removing failed. " (:error response))))}))

(rum/defc form-general < rum/static [config services]
  (comp/card
    {:className "Swarmpit-form-card"}
    (comp/card-header
      {:title     (:configName config)
       :className "Swarmpit-form-card-header Swarmpit-card-header-responsive-title"
       :action    (comp/tooltip
                    {:title "Delete config"}
                    (comp/icon-button
                      {:aria-label "Delete"
                       :onClick    #(state/update-value [:open] true dialog/dialog-cursor)}
                      (comp/svg icon/trash-path)))})
    (comp/card-content
      {}
      (html
        (if (empty? services)
          [:span "Config is not used by any service"]
          [:span "Config is used within " [:b (count services)] " " (inflect/pluralize-noun (count services) "service")])))
    (comp/divider
      {})
    (comp/card-content
      {:style {:paddingBottom "16px"}}
      (form/item-date (:createdAt config) (:updatedAt config))
      (form/item-id (:id config)))))

(def mixin-init-editor
  {:did-mount
   (fn [state]
     (editor/view editor-id)
     state)})

(rum/defc form-config < rum/static
                        mixin-init-editor [config]
  (comp/card
    {:className "Swarmpit-form-card"}
    (comp/card-header
      {:title     (comp/typography {:variant "h6"} "Data")
       :className "Swarmpit-form-table-header"})
    (form-data (parse-data (:data config)))))

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (config-handler id)
      (config-services-handler id))))

(defn form-general-grid [config services]
  (comp/grid
    {:item true
     :xs   12}
    (form-general config services)))

(defn form-config-grid [config]
  (comp/grid
    {:item true
     :xs   12}
      (form-config config)))

(defn form-services-grid [services]
  (comp/grid
    {:item true
     :xs   12}
    (services/linked services)))

(rum/defc form-info < rum/static [{:keys [config services]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       (dialog/confirm-dialog
         #(delete-config-handler (:id config))
         "Delete config?"
         "Delete")
       [:div.Swarmpit-form-context
        (comp/hidden
          {:xsDown         true
           :implementation "js"}
          (comp/grid
            {:container true
             :spacing   16}
            (comp/grid
              {:item true
               :sm   6
               :md   4}
              (comp/grid
                {:container true
                 :spacing   16}
                (form-general-grid config services)))
            (comp/grid
              {:item true
               :sm   6
               :md   8}
              (comp/grid
                {:container true
                 :spacing   16}
                (form-services-grid services)
                (form-config-grid config)))))
        (comp/hidden
          {:smUp           true
           :implementation "js"}
          (comp/grid
            {:container true
             :spacing   16}
            (form-general-grid config services)
            (form-services-grid services)
            (form-config-grid config)))]])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))
