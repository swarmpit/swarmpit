(ns swarmpit.component.registry-acr.edit
  (:require [material.components :as comp]
            [material.component.composite :as composite]
            [swarmpit.component.common :as common]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn- form-principal-id [id]
  (comp/text-field
    {:label           "Service Principal ID"
     :fullWidth       true
     :key             "id"
     :variant         "outlined"
     :margin          "normal"
     :defaultValue    id
     :required        true
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:spId] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-principal-password [password show-password?]
  (comp/text-field
    {:label           "Service Principal Password"
     :variant         "outlined"
     :key             "password"
     :fullWidth       true
     :required        true
     :margin          "normal"
     :type            (if show-password?
                        "text"
                        "password")
     :defaultValue    password
     :onChange        #(state/update-value [:spPassword] (-> % .-target .-value) state/form-value-cursor)
     :InputLabelProps {:shrink true}
     :InputProps      {:className    "Swarmpit-form-input"
                       :endAdornment (common/show-password-adornment show-password?)}}))

(defn- form-public [value]
  (comp/checkbox
    {:checked  value
     :value    (str value)
     :onChange #(state/update-value [:public] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- acr-handler
  [acr-id]
  (ajax/get
    (routes/path-for-backend :registry {:id           acr-id
                                        :registryType :acr})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(defn- update-acr-handler
  [acr-id]
  (ajax/post
    (routes/path-for-backend :registry {:id           acr-id
                                        :registryType :acr})
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :registry-info {:registryType :acr
                                                                 :id           acr-id})))
                   (message/info
                     (str "Registry " acr-id " has been updated.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Registry update failed. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:loading?     true
                    :processing?  false
                    :showPassword false} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (acr-handler id))))

(rum/defc form-edit < rum/static [{:keys [_id spName spId spPassword public]}
                                  {:keys [processing? showPassword]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        [:div.Swarmpit-form-paper
         (common/edit-title (str "Editing " spName))
         [:div.Swarmpit-registry-form
          (comp/grid
            {:container true
             :className "Swarmpit-form-main-grid"
             :spacing   24}
            (comp/grid
              {:item true
               :xs   12}
              (form-principal-id spId)
              (form-principal-password spPassword showPassword))
            (comp/grid
              {:item true
               :xs   12}
              (comp/form-control-label
                {:control (form-public public)
                 :label   "Share"}))
            (comp/grid
              {:item true
               :xs   12}
              (html
                [:div.Swarmpit-form-buttons
                 (composite/progress-button
                   "Save"
                   #(update-acr-handler _id)
                   processing?)])))]]]])))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        registry (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit registry state))))
