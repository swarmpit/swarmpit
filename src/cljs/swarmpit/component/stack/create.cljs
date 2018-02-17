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
            [swarmpit.component.parser :refer [yaml->json json->yaml]]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(defonce cm (atom nil))

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
  (comp/mui
    (comp/text-field
      {:id            editor-id
       :name          "stack-editor"
       :key           "stack-editor"
       :multiLine     true
       :rows          10
       :value         value
       :underlineShow false
       :fullWidth     true})))

(defn- create-stack-handler
  []
  (handler/post
    (routes/path-for-backend :stack-create)
    {:params     (state/get-value cursor)
     :on-success (fn [response]
                   (message/info response))
     :on-error   (fn [response]
                   (message/error (:error response)))}))

(defn- on-change!
  [cm]
  (state/update-value [:compose] (-> cm .getValue) cursor))

(def mixin-init-editor
  {:did-mount
   (fn [state]
     (let [editor (editor/yaml editor-id)]
       (.on editor "change" (fn [cm] (on-change! cm)))
       (reset! cm editor)) state)})

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
       (form-name name))
     (form-editor compose)]))