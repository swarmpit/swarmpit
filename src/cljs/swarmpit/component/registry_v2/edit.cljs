(ns swarmpit.component.registry-v2.edit
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

(defn- form-url [value]
  (comp/text-field
    {:label           "Url"
     :fullWidth       true
     :name            "url"
     :key             "url"
     :variant         "outlined"
     :defaultValue    value
     :required        true
     :InputLabelProps {:shrink true}}))

(defn- form-auth [value]
  (comp/switch
    {:name     "authentication"
     :label    "Authentication"
     :color    "primary"
     :value    (str value)
     :checked  value
     :onChange #(state/update-value [:withAuth] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- form-username [value]
  (comp/text-field
    {:label           "Username"
     :fullWidth       true
     :name            "username"
     :key             "username"
     :variant         "outlined"
     :defaultValue    value
     :margin          "normal"
     :required        true
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:username] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-password [value show-password?]
  (comp/text-field
    {:label           "New Password"
     :variant         "outlined"
     :fullWidth       true
     :type            (if show-password?
                        "text"
                        "password")
     :defaultValue    value
     :margin          "normal"
     :onChange        #(state/update-value [:password] (-> % .-target .-value) state/form-value-cursor)
     :InputLabelProps {:shrink true}
     :InputProps      {:className    "Swarmpit-form-input"
                       :endAdornment (common/show-password-adornment show-password?)}}))

(defn- form-public [value]
  (comp/checkbox
    {:checked  value
     :value    (str value)
     :onChange #(state/update-value [:public] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- registry-handler
  [registry-id]
  (ajax/get
    (routes/path-for-backend :registry {:id registry-id})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(defn- update-registry-handler
  [registry-id]
  (ajax/post
    (routes/path-for-backend :registry-update {:id registry-id})
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :reg-v2-info {:id registry-id})))
                   (message/info
                     (str "Registry " registry-id " has been updated.")))
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
      (registry-handler id))))

(rum/defc form-edit < rum/static [{:keys [_id name url public username password withAuth]}
                                  {:keys [processing? showPassword]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context

        [:div.Swarmpit-form-paper
         (common/edit-title (str "Editing " name))
         [:div.Swarmpit-registry-form
          ;(form-url url)
          ;(comp/form-control
          ;  {:component "fieldset"}
          ;  (comp/form-group
          ;    {}
          ;    (comp/form-control-label
          ;      {:control (form-public public)
          ;       :label   "Share"})))
          ;(comp/form-control
          ;  {:component "fieldset"}
          ;  (comp/form-group
          ;    {}
          ;    (comp/form-control-label
          ;      {:control (form-auth withAuth)
          ;       :label   "Secured"})))
          ;(when withAuth
          ;  (html
          ;    [:div
          ;     (form-username username)
          ;     (form-password password showPassword)]))
          ;[:div.Swarmpit-form-buttons
          ; (composite/progress-button
          ;   "Save"
          ;   #(update-registry-handler _id)
          ;   processing?)]


          (comp/grid
            {:container true
             :className "Swarmpit-form-main-grid"
             :spacing   24}
            (comp/grid
              {:item true
               :xs   12}
              (form-url url))
            (comp/grid
              {:item true
               :xs   12}
              (comp/form-control
                {:component "fieldset"}
                (comp/form-group
                  {}
                  (comp/form-control-label
                    {:control (form-public public)
                     :label   "Share"})))
              (comp/form-control
                {:component "fieldset"}
                (comp/form-group
                  {}
                  (comp/form-control-label
                    {:control (form-auth withAuth)
                     :label   "Secured"}))))
            (when withAuth
              (comp/grid
                {:item true
                 :xs   12}
                (html
                  [:div
                   (form-username username)
                   (form-password password showPassword)])))
            (comp/grid
              {:item true
               :xs   12}
              (html
                [:div.Swarmpit-form-buttons
                 (composite/progress-button
                   "Save"
                   #(update-registry-handler _id)
                   processing?)])))
          ]]]])))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        registry (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit registry state))))
