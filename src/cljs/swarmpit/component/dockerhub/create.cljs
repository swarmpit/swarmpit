(ns swarmpit.component.dockerhub.create
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

(defn- add-user-handler
  []
  (ajax/post
    (routes/path-for-backend :dockerhub-user-create)
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :dockerhub-user-info (select-keys response [:id]))))
                   (message/info
                     (str "User " (:id response) " has been added.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "User cannot be added. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:valid?       false
                    :processing?  false
                    :showPassword false} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:username ""
                    :password ""
                    :public   false} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (init-form-value))))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [{:keys [username password public]} (state/react state/form-value-cursor)
        {:keys [valid? processing? showPassword]} (state/react state/form-state-cursor)]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (comp/grid
            {:item true
             :xs   12
             :sm   6}
            (comp/card
              {:className "Swarmpit-form-card"}
              (comp/card-header
                {:className "Swarmpit-form-card-header"
                 :title     "New Hub Account"})
              (comp/card-content
                {}
                (comp/grid
                  {:container true
                   :spacing   40}
                  (comp/grid
                    {:item true
                     :xs   12}
                    (form-username username)
                    (form-password password showPassword)
                    (comp/form-control
                      {:component "fieldset"}
                      (comp/form-group
                        {}
                        (comp/form-control-label
                          {:control (form-public public)
                           :label   "Public"})))))
                (html
                  [:div.Swarmpit-form-buttons
                   (comp/button
                     {:variant "contained"
                      :onClick add-user-handler
                      :color   "primary"} "Add Dockerhub user")]))))]]))))