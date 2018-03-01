(ns swarmpit.component.stack.edit
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.editor :as editor]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(defonce valid? (atom false))

(defonce loading? (atom false))

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
  (handler/post
    (routes/path-for-backend :stack-update {:name name})
    {:params     (state/get-value cursor)
     :on-success (fn [_]
                   (message/info
                     (str "Stack " name " update triggered.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Stack update failed. " (:error response))))}))

(defn- stackfile-handler
  [name]
  (handler/get
    (routes/path-for-backend :stack-file {:name name})
    {:state      loading?
     :on-success (fn [response]
                   (state/set-value response cursor))}))

(def mixin-init-editor
  {:did-mount
   (fn [state]
     (let [editor (editor/yaml editor-id)]
       (.on editor "change" (fn [cm] (state/update-value [:spec :compose] (-> cm .getValue) cursor))))
     state)})

(defn- init-state
  [name]
  (state/set-value {:name name
                    :spec {:compose ""}} cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [name]} :params}]
      (init-state name)
      (stackfile-handler name))))

(rum/defc form-edit < rum/reactive
                      mixin-init-editor [{:keys [name spec]}]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/stacks name)]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:label      "Deploy"
          :onTouchTap #(update-stack-handler name)
          :disabled   (not (rum/react valid?))
          :primary    true}))]]
   (form/form
     {:onValid   #(reset! valid? true)
      :onInvalid #(reset! valid? false)}
     (form-name name)
     (form-editor (:compose spec)))])

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [stackfile (state/react cursor)]
    (progress/form
      (rum/react loading?)
      (form-edit stackfile))))