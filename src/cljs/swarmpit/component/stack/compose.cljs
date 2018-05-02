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
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def editor-id "compose")

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

(defn- init-form-state
  []
  (state/set-value {:valid?      false
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
      (compose-handler name))))

(rum/defc form-edit < mixin-init-editor [{:keys [name spec]}
                                         {:keys [processing? valid?]}]
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
     (form-editor (:compose spec)))])

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        stackfile (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit stackfile state))))