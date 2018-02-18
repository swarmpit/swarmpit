(ns swarmpit.component.stack.create
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.editor :as editor]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(defonce valid? (atom false))

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
                   (state/update-value [:name] v cursor))})))

(defn- form-editor [value]
  (comp/vtext-field
    {:id            editor-id
     :name          "stack-editor"
     :key           "stack-editor"
     :validations   "isValidCompose"
     :multiLine     true
     :rows          10
     :value         value
     :underlineShow false
     :fullWidth     true}))

(defn- create-stack-handler
  []
  (let [state (state/get-value cursor)]
    (handler/post
      (routes/path-for-backend :stack-create)
      {:params     state
       :on-success (fn [response]
                     (dispatch!
                       (routes/path-for-frontend :stack-info (select-keys state [:name])))
                     (message/info (:result response)))
       :on-error   (fn [response]
                     (message/error (:error response)))})))

(def mixin-init-editor
  {:did-mount
   (fn [state]
     (let [editor (editor/yaml editor-id)]
       (.on editor "change" (fn [cm] (state/update-value [:compose] (-> cm .getValue) cursor))))
     state)})

(defn- init-state
  []
  (state/set-value {:name    ""
                    :compose ""} cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-state))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin-init-editor [_]
  (let [{:keys [compose name]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/stacks "New stack")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Deploy"
            :onTouchTap #(create-stack-handler)
            :disabled   (not (rum/react valid?))
            :primary    true}))]]
     (form/form
       {:onValid   #(reset! valid? true)
        :onInvalid #(reset! valid? false)}
       (form-name name)
       (form-editor compose))]))