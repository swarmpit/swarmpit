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
            [swarmpit.component.parser :refer [yaml->json json->yaml]]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(defonce cm (atom nil))

(defonce loading? (atom false))

(def editor-id "compose")

(defn- form-name [value]
  (comp/mui
    (form/comp
      "STACK NAME"
      (comp/text-field
        {:name          "stack-name"
         :key           "stack-name"
         :underlineShow false
         :disabled      true
         :value         value}))))

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

(defn- update-stack-handler
  [name]
  (handler/post
    (routes/path-for-backend :stack-update {:name name})
    {:params     (state/get-value cursor)
     :on-success (fn [response]
                   (message/info response))
     :on-error   (fn [response]
                   (message/error (:error response)))}))

(defn- stackfile-handler
  [name]
  (handler/get
    (routes/path-for-backend :stack-file {:name name})
    {:state      loading?
     :on-success (fn [response]
                   (state/set-value response cursor))}))

(defn- on-change!
  [cm]
  (state/update-value [:compose] (-> cm .getValue) cursor))

(def mixin-init-editor
  {:did-mount
   (fn [state]
     (let [editor (editor/yaml editor-id)]
       (.on editor "change" (fn [cm] (on-change! cm)))
       (reset! cm editor)) state)})

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [name]} :params}]
      (stackfile-handler name))))

(rum/defc form-edit < rum/static
                      mixin-init-editor [{:keys [name compose]}]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/stacks name)]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:label      "Redeploy"
          :onTouchTap #(update-stack-handler name)
          :primary    true}))]]
   (form-name name)
   (form-editor compose)])

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [stackfile (state/react cursor)]
    (progress/form
      (rum/react loading?)
      (form-edit stackfile))))

