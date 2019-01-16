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
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
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
     :margin          "normal"
     :defaultValue    value
     :required        true
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
               :key  "stccgif"
               :xs   12
               :sm   12
               :md   12
               :lg   8
               :xl   8}
              (comp/card
                {:className "Swarmpit-form-card"
                 :key       "scfcc"}
                (comp/card-header
                  {:className "Swarmpit-form-card-header"
                   :key       "scfcch"
                   :title     "New Stack"})
                (comp/card-content
                  {:key "scfccc"}
                  (comp/grid
                    {:container true
                     :key       "scfcccc"
                     :spacing   40}
                    (comp/grid
                      {:item true
                       :key  "scfccccig"
                       :xs   12
                       :lx   4}
                      (form-name name))
                    (comp/grid
                      {:item true
                       :key  "scfccccie"
                       :xs   12
                       :lx   4}
                      (when-not from
                        (html [:span {:class "Swarmpit-message"
                                      :key   "scfcccciem"}
                               "Drag & drop or paste a compose file."]))
                      (form-editor (:compose spec))))
                  (html
                    [:div {:class "Swarmpit-form-buttons"
                           :key   "scfcccbtn"}
                     (composite/progress-button
                       "Deploy"
                       create-stack-handler
                       processing?)]))))
            (comp/grid
              {:item true
               :key  "stccgid"
               :xs   12
               :sm   12
               :md   12
               :lg   4
               :xl   4}
              (form/open-in-new "Learn more about compose" doc-compose-link)
              (form/open-in-new "Format reference" doc-compose-ref-link)))]]))))

(rum/defc form < rum/reactive
                 mixin-init-form [params]
  (let [state (state/react state/form-state-cursor)]
    (progress/form
      (:loading? state)
      (form-edit params))))
