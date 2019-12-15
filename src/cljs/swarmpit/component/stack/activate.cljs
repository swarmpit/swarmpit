(ns swarmpit.component.stack.activate
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.composite :as composite]
            [swarmpit.component.editor :as editor]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.dialog :as dialog]
            [swarmpit.component.common :as common]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(def editor-id "compose")

(def doc-compose-link "https://docs.docker.com/get-started/part3/#your-first-docker-composeyml-file")

(defn- form-name [value]
  (comp/text-field
    {:label           "Name"
     :fullWidth       true
     :name            "name"
     :key             "name"
     :variant         "outlined"
     :defaultValue    value
     :required        true
     :disabled        true
     :margin          "normal"
     :InputLabelProps {:shrink true}}))

(defn- form-editor [value]
  (comp/text-field
    {:id              editor-id
     :fullWidth       true
     :className       "Swarmpit-codemirror"
     :name            "config-view"
     :key             "config-view"
     :multiline       true
     :disabled        true
     :required        true
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :value           value}))

(defn- deploy-stack-handler
  [name]
  (ajax/post
    (routes/path-for-backend :stack {:name name})
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :stack-info {:name name})))
                   (message/info
                     (str "Stack " name " has been deployed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Stack deploy failed. " (:error response))))}))

(defn- delete-stackfile-handler
  [name]
  (ajax/delete
    (routes/path-for-backend :stack-file {:name name})
    {:state      [:deleting?]
     :on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :stack-list))
                   (message/info
                     (str "Stackfile " name " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Stackfile removal failed. " (:error response))))}))

(defn stackfile-handler
  [name]
  (ajax/get
    (routes/path-for-backend :stack-file {:name name})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:spec] (:spec response) state/form-value-cursor))}))

(defn- init-form-value
  [name]
  (state/set-value {:name name
                    :spec {:compose ""}} state/form-value-cursor))

(def mixin-init-editor
  {:did-mount
   (fn [state]
     (let [editor (editor/yaml editor-id)]
       (.on editor "change" (fn [cm] (state/update-value [:spec :compose] (-> cm .getValue) state/form-value-cursor))))
     state)})

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [name]} :params}]
      (init-form-value name)
      (stackfile-handler name))))

(rum/defc form-edit < rum/reactive
                      mixin-init-editor [{:keys [name spec]}
                                         {:keys [processing? deleting?]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       (dialog/confirm-dialog
         #(delete-stackfile-handler name)
         "Delete stackfile?"
         "Delete")
       [:div.Swarmpit-form-context
        (comp/container
          {:maxWidth  "md"
           :className "Swarmpit-container"}
          (comp/card
            {:className "Swarmpit-form-card Swarmpit-fcard"}
            (comp/box
              {:className "Swarmpit-fcard-header"}
              (comp/typography
                {:className "Swarmpit-fcard-header-title"
                 :variant   "h6"
                 :component "div"}
                "Activate stack"))
            (comp/card-content
              {:className "Swarmpit-fcard-content"}
              (form-name name)
              (form-editor (:compose spec)))
            (comp/card-actions
              {:className "Swarmpit-fcard-actions"}
              (composite/progress-button
                "Deploy"
                #(deploy-stack-handler name)
                processing?
                false
                {:startIcon (comp/svg {} icon/rocket-path)})
              (comp/button
                {:color     "default"
                 :variant   "outlined"
                 :startIcon (comp/svg {} icon/trash-path)
                 :disabled  processing?
                 :onClick   #(state/update-value [:open] true dialog/dialog-cursor)}
                "Delete"))))]])))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        stackfile (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit stackfile state))))
