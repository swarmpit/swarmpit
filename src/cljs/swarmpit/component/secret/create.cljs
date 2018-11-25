(ns swarmpit.component.secret.create
  (:require [material.component :as comp]
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

(defn- form-name [value]
  (comp/text-field
    {:label           "Name"
     :fullWidth       true
     :name            "name"
     :key             "name"
     :variant         "outlined"
     :value           value
     :required        true
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:secretName] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-data [value]
  (comp/text-field
    {:id              editor-id
     :fullWidth       true
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
  (state/set-value {:secretName nil
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
          (comp/paper
            {:className "Swarmpit-paper Swarmpit-form-context"
             :elevation 0}
            (comp/grid
              {:container true
               :spacing   40}
              (comp/grid
                {:item true
                 :xs   12
                 :sm   6}
                (form-name secretName))
              (comp/grid
                {:item true
                 :xs   12}
                (form-data data)))
            (html
              [:div.Swarmpit-form-buttons
               (comp/button
                 {:variant "contained"
                  :onClick create-secret-handler
                  :color   "primary"} "Create")]))]]))))
