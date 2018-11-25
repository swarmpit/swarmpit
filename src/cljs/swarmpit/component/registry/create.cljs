(ns swarmpit.component.registry.create
  (:require [material.icon :as icon]
            [material.component :as comp]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
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
     :value           value
     :required        true
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:name] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-url [value]
  (comp/text-field
    {:label           "Url"
     :fullWidth       true
     :name            "url"
     :key             "url"
     :variant         "outlined"
     :value           value
     :required        true
     :margin          "normal"
     :placeholder     "e.g. https://my.registry.io"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:url] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-auth [value]
  (comp/switch
    {:name     "authentication"
     :label    "Authentication"
     :color    "primary"
     :value    value
     :checked  value
     :onChange #(state/update-value [:withAuth] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- form-public [value]
  (comp/checkbox
    {:checked  value
     :value    value
     :onChange #(state/update-value [:public] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- form-username [value]
  (comp/text-field
    {:label           "Name"
     :fullWidth       true
     :name            "username"
     :key             "username"
     :variant         "outlined"
     :margin          "normal"
     :value           value
     :required        true
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:username] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-password-adornment [show-password?]
  (comp/input-adornment
    {:position "end"}
    (comp/icon-button
      {:aria-label  "Toggle password visibility"
       :onClick     #(state/update-value [:showPassword] (not show-password?) state/form-state-cursor)
       :onMouseDown (fn [event]
                      (.preventDefault event))}
      (if show-password?
        icon/visibility-off
        icon/visibility))))

(defn- form-password [value show-password?]
  (comp/text-field
    {:label           "Password"
     :variant         "outlined"
     :fullWidth       true
     :required        true
     :margin          "normal"
     :type            (if show-password?
                        "text"
                        "password")
     :value           value
     :onChange        #(state/update-value [:password] (-> % .-target .-value) state/form-value-cursor)
     :InputLabelProps {:shrink true}
     :InputProps      {:endAdornment (form-password-adornment show-password?)}}))

(defn- form-public [value]
  (comp/checkbox
    {:checked  value
     :value    value
     :onChange #(state/update-value [:public] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- create-registry-handler
  []
  (ajax/post
    (routes/path-for-backend :registry-create)
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :registry-info (select-keys response [:id]))))
                   (message/info
                     (str "Registry " (:id response) " has been created.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Registry creation failed. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:valid?       false
                    :processing?  false
                    :showPassword false} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:name     ""
                    :url      ""
                    :public   false
                    :withAuth false
                    :username ""
                    :password ""} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (init-form-value))))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [{:keys [name url public withAuth username password]} (state/react state/form-value-cursor)
        {:keys [valid? processing? showPassword]} (state/react state/form-state-cursor)]
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
                (form-name name)
                (form-url url)
                (comp/form-control
                  {:component "fieldset"}
                  (comp/form-group
                    {}
                    (comp/form-control-label
                      {:control (form-public public)
                       :label   "Public"})))
                (comp/form-control
                  {:component "fieldset"}
                  (comp/form-group
                    {}
                    (comp/form-control-label
                      {:control (form-auth withAuth)
                       :label   "Authentication"})))
                (when withAuth
                  (html
                    [:div
                     (form-username username)
                     (form-password password showPassword)]))))
            (html
              [:div.Swarmpit-form-buttons
               (comp/button
                 {:variant "contained"
                  :onClick create-registry-handler
                  :color   "primary"} "Add registry")]))]]))))