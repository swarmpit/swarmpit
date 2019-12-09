(ns swarmpit.component.stack.create
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.composite :as composite]
            [swarmpit.component.editor :as editor]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.common :as common]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [clojure.string :as str]
            [rum.core :as rum]))

(enable-console-print!)

(def editor-id "compose")

(def doc-compose-link "https://docs.docker.com/get-started/part3/#your-first-docker-composeyml-file")

(def doc-compose-ref-link "https://docs.docker.com/compose/compose-file")

(defn- form-name [value]
  (comp/text-field
    {:label           "Name"
     :fullWidth       true
     :name            "name"
     :key             "name"
     :variant         "outlined"
     :helperText      "Specify stack name"
     :defaultValue    value
     :required        true
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:name] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-editor [value]
  (comp/text-field
    {:id              editor-id
     :fullWidth       true
     :className       "Swarmpit-codemirror"
     :name            "data"
     :key             "data"
     :multiline       true
     :disabled        true
     :required        true
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :value           value}))

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
      (routes/path-for-backend :stacks)
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

(defn- save-stack-handler
  []
  (let [{:keys [name] :as form-value} (state/get-value state/form-value-cursor)]
    (ajax/post
      (routes/path-for-backend :stack-file {:name name})
      {:params     form-value
       :state      [:saving?]
       :on-success (fn [{:keys [origin?]}]
                     (when origin?
                       (dispatch!
                         (routes/path-for-frontend :stack-list)))
                     (message/info
                       (str "Stack " name " succesfully saved.")))
       :on-error   (fn [{:keys [response]}]
                     (message/error
                       (str "Stack save failed. " (:error response))))})))

(def mixin-init-editor
  {:did-mount
   (fn [state]
     (let [editor (editor/yaml editor-id)]
       (.on editor "change" (fn [cm] (state/update-value [:spec :compose] (-> cm .getValue) state/form-value-cursor))))
     state)})

(defn- init-form-state
  []
  (state/set-value {:valid?      false
                    :processing? false
                    :saving?     false} state/form-state-cursor))

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
        {:keys [valid? processing? saving?]} (state/react state/form-state-cursor)]
    (comp/mui
      (html
        [:div.Swarmpit-form
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
                  "Create stack"))
              (comp/card-content
                {:className "Swarmpit-fcard-content"}
                (comp/typography
                  {:variant   "body2"
                   :className "Swarmpit-fcard-message"}
                  "Group of interrelated services that are orchestrated and scaled together")
                (form-name name)
                (form-editor (:compose spec)))
              (comp/card-actions
                {:className "Swarmpit-fcard-actions"}
                (composite/progress-button
                  "Deploy"
                  create-stack-handler
                  processing?
                  saving?
                  {:startIcon (comp/svg {} icon/rocket-path)})
                (composite/progress-button
                  "Save"
                  save-stack-handler
                  saving?
                  processing?
                  {:variant   "outlined"
                   :color     "secondary"
                   :startIcon (icon/save {})}))))]]))))

(rum/defc form < rum/reactive
                 mixin-init-form [params]
  (let [state (state/react state/form-state-cursor)]
    (progress/form
      (:loading? state)
      (form-edit params))))
