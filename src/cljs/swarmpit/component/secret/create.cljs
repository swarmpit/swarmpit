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
            [sablono.core :refer-macros [html]]
            [swarmpit.url :refer [dispatch!]]
            [rum.core :as rum]))

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
     :required        true
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:secretName] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-data [value]
  (comp/text-field
    {:id              editor-id
     :fullWidth       true
     :className       "Swarmpit-codemirror"
     :name            "data"
     :key             "data"
     :variant         "outlined"
     :required        true
     :InputLabelProps {:shrink true}
     :value           value}))

(defn- create-secret-handler
  []
  (ajax/post
    (routes/path-for-backend :secret-create)
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :secret-info (select-keys response [:id]))))
                   (message/info
                     (str "Secret " (:id response) " has been created.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Secret creation failed. " (:error response))))}))

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
          (comp/grid
            {:container true
             :key       "sccg"
             :spacing   40}
            (comp/grid
              {:item true
               :key  "ssecoccgif"
               :xs   12
               :sm   12
               :md   12
               :lg   8
               :xl   8}
              (comp/card
                {:className "Swarmpit-form-card"
                 :key       "scc"}
                (comp/card-header
                  {:className "Swarmpit-form-card-header"
                   :key       "scch"
                   :title     "Create Secret"})
                (comp/card-content
                  {:key "sccc"}
                  (comp/grid
                    {:container true
                     :key       "scccc"
                     :spacing   40}
                    (comp/grid
                      {:item true
                       :key  "scccig"
                       :xs   12}
                      (form-name secretName))
                    (comp/grid
                      {:item true
                       :key  "scccid"
                       :xs   12}
                      (form-data data)))
                  (html
                    [:div {:class "Swarmpit-form-buttons"
                           :key   "scccbtn"}
                     (composite/progress-button
                       "Create"
                       create-secret-handler
                       processing?)]))))
            (comp/grid
              {:item true
               :key  "ssecoccgid"
               :xs   12
               :sm   12
               :md   12
               :lg   4
               :xl   4}
              (form/open-in-new "Learn more about secrets" doc-secrets-link)))]]))))
