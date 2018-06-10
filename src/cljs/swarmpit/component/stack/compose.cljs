(ns swarmpit.component.stack.compose
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.editor :as editor]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.create-image :as images]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def editor-id "compose")

(defn tab-style [disabled?]
  (if disabled?
    {:color    "rgba(0, 0, 0, 0.3)"
     :minWidth "200px"}
    images/tab-style))

(defn- form-name [value]
  (form/comp
    "STACK NAME"
    (comp/vtext-field
      {:name     "stack-name"
       :key      "stack-name"
       :required true
       :disabled true
       :value    value})))

(defn- form-editor [value]
  (comp/vtext-field
    {:id            editor-id
     :name          "stack-editor"
     :key           "stack-editor"
     :validations   "isValidCompose"
     :multiLine     true
     :rows          10
     :rowsMax       10
     :value         value
     :underlineShow false
     :fullWidth     true}))

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

(defn tabs
  [name value last? previous?]
  [:div.form-panel-tabs
   (comp/mui
     (comp/tabs
       {:key                   "tabs"
        :value                 value
        :inkBarStyle           images/tabs-inkbar-style
        :tabItemContainerStyle images/tabs-container-style}
       (comp/tab
         {:key   "compose"
          :href  (routes/path-for-frontend :stack-compose {:name name})
          :label "Current state"
          :style (tab-style false)
          :value 0})
       (comp/tab
         {:key      "last"
          :href     (routes/path-for-frontend :stack-last {:name name})
          :label    "Last deployed"
          :style    (tab-style (not last?))
          :disabled (not last?)
          :value    1})
       (comp/tab
         {:key      "previous"
          :href     (routes/path-for-frontend :stack-previous {:name name})
          :label    "Previously deployed"
          :style    (tab-style (not previous?))
          :disabled (not previous?)
          :value    2})))])

(rum/defc form-edit [{:keys [name spec]}
                     {:keys [processing? valid? last? previous?]}]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/stacks name)]
    [:div.form-panel-right
     (comp/progress-button
       {:label      "Deploy"
        :disabled   (not valid?)
        :primary    true
        :onTouchTap #(update-stack-handler name)} processing?)]]
   (form/form
     {:onValid   #(state/update-value [:valid?] true state/form-state-cursor)
      :onInvalid #(state/update-value [:valid?] false state/form-state-cursor)}
     (form-name name)
     (html (tabs name 0 last? previous?))
     (editor (:compose spec)))])

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        stackfile (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit stackfile state))))