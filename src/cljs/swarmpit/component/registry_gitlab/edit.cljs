(ns swarmpit.component.registry-gitlab.edit
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

(defn- form-token [value show-token?]
  (comp/text-field
    {:label           "New Personal Access Token"
     :variant         "outlined"
     :fullWidth       true
     :required        true
     :margin          "normal"
     :type            (if show-token?
                        "text"
                        "password")
     :defaultValue    value
     :onChange        #(state/update-value [:token] (-> % .-target .-value) state/form-value-cursor)
     :InputLabelProps {:shrink true}
     :InputProps      {:className    "Swarmpit-form-input"
                       :endAdornment (common/show-password-adornment show-token? :showToken)}}))

(defn- form-public [value]
  (comp/checkbox
    {:checked  value
     :value    (str value)
     :onChange #(state/update-value [:public] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- registry-handler
  [registry-id]
  (ajax/get
    (routes/path-for-backend :registry {:id           registry-id
                                        :registryType :gitlab})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(defn- update-registry-handler
  [registry-id]
  (ajax/post
    (routes/path-for-backend :registry-update {:id           registry-id
                                               :registryType :gitlab})
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :registry-info {:registryType :gitlab
                                                                 :id           registry-id})))
                   (message/info
                     (str "Gitlab registry " registry-id " has been updated.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Gitlab registry update failed. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:loading?    true
                    :processing? false
                    :showToken   false} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (registry-handler id))))

(rum/defc form-edit < rum/static [{:keys [_id username token public]}
                                  {:keys [processing? showToken]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        [:div.Swarmpit-form-paper
         (common/edit-title (str "Editing " username))
         [:div.Swarmpit-registry-form
          (comp/grid
            {:container true
             :className "Swarmpit-form-main-grid"
             :spacing   24}
            (comp/grid
              {:item true
               :xs   12}
              (form-token token showToken))
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
                   #(update-registry-handler _id)
                   processing?)])))]]]])))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        registry (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit registry state))))
