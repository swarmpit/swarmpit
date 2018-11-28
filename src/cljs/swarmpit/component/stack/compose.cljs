(ns swarmpit.component.stack.compose
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.composite :as composite]
            [swarmpit.component.editor :as editor]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def editor-id "compose")

(defn- form-name [value]
  (comp/text-field
    {:label           "Name"
     :fullWidth       true
     :name            "name"
     :key             "name"
     :variant         "outlined"
     :value           value
     :margin          "normal"
     :required        true
     :disabled        true
     :InputLabelProps {:shrink true}}))

(defn- form-editor [value]
  (comp/text-field
    {:id              editor-id
     :fullWidth       true
     :name            "config-view"
     :key             "config-view"
     :multiline       true
     :disabled        true
     :required        true
     :InputLabelProps {:shrink true}
     :value           value}))

(defn- update-stack-handler
  [name]
  (ajax/post
    (routes/path-for-backend :stack-update {:name name})
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :stack-info {:name name})))
                   (message/info
                     (str "Stack " name " has been updated.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Stack update failed. " (:error response))))}))

(defn- compose-handler
  [name]
  (ajax/get
    (routes/path-for-backend :stack-compose {:name name})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(def mixin-init-editor
  {:did-mount
   (fn [state]
     (let [editor (editor/yaml editor-id)]
       (.on editor "change" (fn [cm] (state/update-value [:spec :compose] (-> cm .getValue) state/form-value-cursor))))
     state)})

(defn stackfile-handler
  [name]
  (ajax/get
    (routes/path-for-backend :stack-file {:name name})
    {:on-success (fn [{:keys [response]}]
                   (when (:spec response) (state/update-value [:last?] true state/form-state-cursor))
                   (when (:previousSpec response) (state/update-value [:previous?] true state/form-state-cursor)))
     :on-error   (fn [_]
                   (state/update-value [:last?] false state/form-state-cursor)
                   (state/update-value [:previous?] false state/form-state-cursor))}))

(defn- init-form-state
  []
  (state/set-value {:valid?      false
                    :last?       false
                    :previous?   false
                    :loading?    true
                    :processing? false} state/form-state-cursor))

(defn- init-form-value
  [name]
  (state/set-value {:name name
                    :spec {:compose ""}} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [name]} :params}]
      (init-form-state)
      (init-form-value name)
      (stackfile-handler name)
      (compose-handler name))))

(rum/defc editor < mixin-init-editor [spec]
  (form-editor spec))

(defn- form-select [name value last? previous?]
  (print value)
  (comp/text-field
    {:fullWidth       true
     :label           "Compose file"
     :helperText      "Compose file source"
     :select          true
     :value           value
     :variant         "outlined"
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        #(dispatch! (routes/path-for-frontend (keyword (-> % .-target .-value)) {:name name}))}
    (comp/menu-item
      {:key   "current"
       :value :current} "Current engine state")
    (comp/menu-item
      {:key      "last"
       :value    :last
       :disabled (not last?)} "Last deployed")
    (comp/menu-item
      {:key      "previous"
       :value    :previous
       :disabled (not previous?)} "Previously deployed (rollback)")))

(rum/defc form-edit < rum/reactive
                      mixin-init-editor
  [{:keys [name spec]}
   {:keys [processing? valid? last? previous?]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (comp/card
          {:className "Swarmpit-form-card"}
          (comp/card-header
            {:className "Swarmpit-form-card-header"
             :title     "Edit Stack"})
          (comp/card-content
            {}
            (comp/grid
              {:container true
               :spacing   40}
              (comp/grid
                {:item true
                 :xs   12}
                (form-name name)
                (form-select name :current last? previous?))
              (comp/grid
                {:item true
                 :xs   12}
                (form-editor (:compose spec))))
            (html
              [:div.Swarmpit-form-buttons
               (composite/progress-button
                 "Deploy"
                 #(update-stack-handler name)
                 processing?)])))]])))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        stackfile (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit stackfile state))))
