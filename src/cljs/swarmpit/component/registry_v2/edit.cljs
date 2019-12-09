(ns swarmpit.component.registry-v2.edit
  (:require [material.icon :as icon]
            [material.components :as comp]
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

(defn- form-url [value]
  (comp/text-field
    {:label           "Url"
     :fullWidth       true
     :name            "url"
     :key             "url"
     :variant         "outlined"
     :defaultValue    value
     :required        true
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:url] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-custom [value]
  (comp/switch
    {:name     "custom"
     :label    "Custom"
     :color    "primary"
     :value    (str value)
     :checked  value
     :onChange #(state/update-value [:customApi] (-> % .-target .-checked) state/form-value-cursor)}))

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
  (comp/switch
    {:checked  value
     :value    (str value)
     :color    "primary"
     :onChange #(state/update-value [:public] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- registry-handler
  [registry-id]
  (ajax/get
    (routes/path-for-backend :registry {:id           registry-id
                                        :registryType :v2})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/set-value response state/form-value-cursor))}))

(defn- update-registry-handler
  [registry-id]
  (ajax/post
    (routes/path-for-backend :registry {:id           registry-id
                                        :registryType :v2})
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :registry-info {:registryType :v2
                                                                 :id           registry-id})))
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

(rum/defc form-edit < rum/static [{:keys [_id name url public username password withAuth customApi]}
                                  {:keys [processing? showPassword]}]
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (comp/container
          {:maxWidth  "sm"
           :className "Swarmpit-container"}
          (comp/card
            {:className "Swarmpit-form-card Swarmpit-fcard"}
            (comp/box
              {:className "Swarmpit-fcard-header"}
              (comp/typography
                {:className "Swarmpit-fcard-header-title"
                 :variant   "h6"
                 :component "div"}
                "Edit registry"))
            (comp/card-content
              {:className "Swarmpit-fcard-content"}
              (comp/typography
                {:variant   "body2"
                 :className "Swarmpit-fcard-message"}
                "Update Registry v2 account settings")
              (form-name name)
              (form-url url)
              (comp/form-control
                {:component "fieldset"
                 :key       "role-f"
                 :margin    "normal"}
                (comp/form-label
                  {:key "rolel"} "Make account Public")
                (comp/form-helper-text
                  {} "Means that anyone can search & deploy private repositories from this account")
                (comp/form-control-label
                  {:control (form-public public)}))
              (comp/form-control
                {:component "fieldset"
                 :key       "role-f"
                 :margin    "normal"}
                (comp/form-label
                  {:key "rolel"} "Use custom API")
                (comp/form-helper-text
                  {} "Registry URL is not suffixed with /v2/ api path, instead given value is used as based url")
                (comp/form-control-label
                  {:control (form-custom customApi)
                   :label   "Custom API"}))
              (comp/form-control
                {:component "fieldset"
                 :key       "role-f"
                 :margin    "normal"}
                (comp/form-label
                  {:key "rolel"} "Secured access")
                (comp/form-helper-text
                  {} "Use basic auth within the registry")
                (comp/form-control-label
                  {:control (form-auth withAuth)
                   :label   "Secured"}))
              (when withAuth
                (comp/box
                  {}
                  (form-username username)
                  (form-password password showPassword)))
              (comp/box
                {:className "Swarmpit-form-buttons"}
                (composite/progress-button
                  "Save"
                  #(update-registry-handler _id)
                  processing?
                  false
                  {:startIcon (icon/save {})})))))]])))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [state (state/react state/form-state-cursor)
        registry (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-edit registry state))))
