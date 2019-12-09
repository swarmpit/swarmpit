(ns swarmpit.component.secret.create
  (:require [material.components :as comp]
            [material.component.form :as form]
            [material.component.composite :as composite]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.editor :as editor]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.base64 :as base64]
            [sablono.core :refer-macros [html]]
            [swarmpit.url :refer [dispatch!]]
            [rum.core :as rum]
            [swarmpit.component.common :as common]))

(enable-console-print!)

(def editor-id "secret-editor")

(def doc-secrets-link "https://docs.docker.com/engine/swarm/secrets/")

(defn- form-name [value]
  (comp/text-field
    {:label           "Name"
     :fullWidth       true
     :name            "name"
     :key             "name"
     :variant         "outlined"
     :defaultValue    value
     :margin          "normal"
     :required        true
     :helperText      "Specify secret name"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:secretName] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-data [value]
  (comp/text-field
    {:id              editor-id
     :fullWidth       true
     :className       "Swarmpit-codemirror"
     :name            "data"
     :key             "data"
     :required        true
     :multiline       true
     :disabled        true
     :margin          "normal"
     :InputProps      {:style {:padding 0}}
     :InputLabelProps {:shrink true}
     :value           value}))

(defn- create-secret-handler
  []
  (let [req (state/get-value state/form-value-cursor)]
    (ajax/post
      (routes/path-for-backend :secrets)
      {:params     (-> req
                       (assoc-in [:data] (base64/encode (:data req))))
       :state      [:processing?]
       :on-success (fn [{:keys [response origin?]}]
                     (when origin?
                       (dispatch!
                         (routes/path-for-frontend :secret-info (select-keys response [:id]))))
                     (message/info
                       (str "Secret " (:id response) " has been created.")))
       :on-error   (fn [{:keys [response]}]
                     (message/error
                       (str "Secret creation failed. " (:error response))))})))

(defn- init-form-state
  []
  (state/set-value {:valid?      false
                    :processing? false} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:secretName ""
                    :data       ""} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (init-form-value))))

(def mixin-init-editor
  {:did-mount
   (fn [state]
     (let [editor (editor/default editor-id)]
       (.on editor "change" (fn [cm] (state/update-value [:data] (-> cm .getValue) state/form-value-cursor))))
     state)})

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin-init-editor [_]
  (let [{:keys [secretName data encode]} (state/react state/form-value-cursor)
        {:keys [valid? processing?]} (state/react state/form-state-cursor)]
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
                  "Create secret"))
              (comp/card-content
                {:className "Swarmpit-fcard-content"}
                (comp/typography
                  {:variant   "body2"
                   :className "Swarmpit-fcard-message"}
                  "Blob of data, such as a password or SSH private key")
                (form-name secretName)
                (form-data data))
              (comp/card-actions
                {:className "Swarmpit-fcard-actions"}
                (composite/progress-button
                  "Create"
                  create-secret-handler
                  processing?))))]]))))