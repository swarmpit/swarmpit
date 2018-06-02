(ns swarmpit.component.stack.create
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.editor :as editor]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.message :as message]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [swarmpit.component.progress :as progress]))

(enable-console-print!)

(def editor-id "compose")

(defn- form-name [value]
  (form/comp
    "STACK NAME"
    (comp/vtext-field
      {:name     "stack-name"
       :key      "stack-name"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:name] v state/form-value-cursor))})))

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

(defn- compose-handler
  [service-name]
  (when service-name
    (ajax/get
      (routes/path-for-backend :service-compose {:id service-name})
      {:state      [:loading?]
       :on-success (fn [{:keys [response]}]
                     (state/set-value response state/form-value-cursor))
       :on-error   (fn [_]
                     (message/error
                       (str "Failed to generate compose file from service " service-name)))})))

(defn- create-stack-handler
  []
  (let [{:keys [name] :as form-value} (state/get-value state/form-value-cursor)]
    (ajax/post
      (routes/path-for-backend :stack-create)
      {:params     form-value
       :state      [:processing?]
       :on-success (fn [{:keys [origin?]}]
                     (when origin?
                       (dispatch!
                         (routes/path-for-frontend :stack-info {:name name})))
                     (message/info
                       (str "Stack " name " succesfully deployed.")))
       :on-error   (fn [{:keys [response]}]
                     (message/error
                       (str "Stack deployment failed. " (:error response))))})))

(def mixin-init-editor
  {:did-mount
   (fn [state]
     (let [editor (editor/yaml editor-id)]
       (.on editor "change" (fn [cm] (state/update-value [:spec :compose] (-> cm .getValue) state/form-value-cursor))))
     state)})

(defn- init-form-state
  []
  (state/set-value {:valid?      false
                    :processing? false} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:name ""
                    :spec {:compose ""}} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [from]} :params}]
      (init-form-state)
      (init-form-value)
      (compose-handler from))))

(rum/defc form-edit < rum/reactive
                      mixin-init-editor [{{:keys [from]} :params}]
  (let [{:keys [spec name]} (state/react state/form-value-cursor)
        {:keys [valid? processing?]} (state/react state/form-state-cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/stacks "New stack")]
      [:div.form-panel-right
       (comp/progress-button
         {:label      "Deploy"
          :disabled   (not valid?)
          :primary    true
          :onTouchTap create-stack-handler} processing?)]]
     (form/form
       {:onValid   #(state/update-value [:valid?] true state/form-state-cursor)
        :onInvalid #(state/update-value [:valid?] false state/form-state-cursor)}
       (form-name name)
       (when-not from
         (html (form/icon-value icon/info "Please drag & drop or paste a compose file.")))
       (form-editor (:compose spec)))]))

(rum/defc form < rum/reactive
                 mixin-init-form [params]
  (let [state (state/react state/form-state-cursor)]
    (progress/form
      (:loading? state)
      (form-edit params))))