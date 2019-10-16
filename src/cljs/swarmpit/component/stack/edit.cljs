(ns swarmpit.component.stack.edit
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.composite :as composite]
            [swarmpit.component.editor :as editor]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.stack.compose :as compose]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [clojure.set :as set]
            [swarmpit.component.common :as common]))

(enable-console-print!)

(def editor-id "compose")

(def doc-compose-link "https://docs.docker.com/get-started/part3/#your-first-docker-composeyml-file")

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

(defn- update-stack-handler
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
                     (str "Stack " name " has been updated.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Stack update failed. " (:error response))))}))

(defn- stackfile-handler
  [name handler]
  (ajax/get
    (routes/path-for-backend :stack-file {:name name})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value (case handler :stack-previous (set/rename-keys response {:previousSpec :spec}) response) state/form-value-cursor)
                   (state/update-value [:previous?] (:previousSpec response) state/form-state-cursor))}))

(def mixin-init-editor
  {:did-mount
   (fn [state]
     (let [editor (editor/yaml editor-id)]
       (.on editor "change" (fn [cm] (state/update-value [:spec :compose] (-> cm .getValue) state/form-value-cursor))))
     state)})

(defn- init-form-state
  []
  (state/set-value {:valid?      false
                    :previous?   false
                    :loading?    true
                    :processing? false} state/form-state-cursor))

(defn- init-form-value
  [name]
  (state/set-value {:name name
                    :spec {:compose ""}} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [name]} :params :keys [handler]}]
      (init-form-state)
      (init-form-value name)
      (stackfile-handler name handler))))

(rum/defc form-edit < mixin-init-editor [{:keys [name spec]}
                                         select
                                         {:keys [processing? valid? previous?]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        [:div.Swarmpit-form-paper
         (common/edit-title (str "Editing " name))
         (comp/grid
           {:container true
            :className "Swarmpit-form-main-grid"
            :spacing   40}
           (comp/grid
             {:item true
              :xs   12
              :sm   12
              :md   12
              :lg   8
              :xl   8}
             (comp/grid
               {:container true
                :spacing   40}
               (comp/grid
                 {:item true
                  :xs   12}
                 (compose/form-select name select true previous?))
               (comp/grid
                 {:item true
                  :xs   12}
                 (form-editor (:compose spec)))
               (comp/grid
                 {:item true
                  :xs   12}
                 (html
                   [:div.Swarmpit-form-buttons
                    (composite/progress-button
                      "Deploy"
                      #(update-stack-handler name)
                      processing?)]))))
           (comp/grid
             {:item true
              :xs   12
              :sm   12
              :md   12
              :lg   4
              :xl   4}
             (form/open-in-new "Learn more about compose" doc-compose-link)))]]])))

(rum/defc form-last < rum/reactive
                      mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        stackfile (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit stackfile :stack-last state))))

(rum/defc form-previous < rum/reactive
                          mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        stackfile (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit stackfile :stack-previous state))))
