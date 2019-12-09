(ns swarmpit.component.user.create
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.composite :as composite]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [swarmpit.component.common :as common]))

(enable-console-print!)

(defn- form-username [value]
  (comp/text-field
    {:label           "Username"
     :fullWidth       true
     :name            "username"
     :key             "username"
     :variant         "outlined"
     :defaultValue    value
     :required        true
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:username] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-password [value show-password?]
  (comp/text-field
    {:label           "Password"
     :variant         "outlined"
     :key             "password"
     :fullWidth       true
     :required        true
     :type            (if show-password?
                        "text"
                        "password")
     :defaultValue    value
     :onChange        #(state/update-value [:password] (-> % .-target .-value) state/form-value-cursor)
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :InputProps      {:className    "Swarmpit-form-input"
                       :endAdornment (common/show-password-adornment show-password?)}}))

(defn- form-role [value]
  (comp/form-control
    {:component "fieldset"
     :key       "role-f"
     :margin    "normal"}
    (comp/form-label
      {:key "rolel"} "Define Role")
    (comp/form-helper-text
      {} "Specify account priviledges level")
    (comp/radio-group
      {:name     "role"
       :key      "role-rg"
       :value    value
       :onChange #(state/update-value [:role] (-> % .-target .-value) state/form-value-cursor)}
      (comp/form-control-label
        {:control (comp/radio
                    {:name  "user-role"
                     :color "primary"
                     :key   "user-role"})
         :key     "usr-role"
         :value   "user"
         :label   "User"})
      (comp/form-control-label
        {:control (comp/radio
                    {:name  "admin-role"
                     :color "primary"
                     :key   "admin-role"})
         :key     "ad-role"
         :value   "admin"
         :label   "Admin"}))))

(defn- form-email [value]
  (comp/text-field
    {:label           "Email"
     :fullWidth       true
     :key             "email"
     :variant         "outlined"
     :defaultValue    value
     :required        true
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:email] (-> % .-target .-value) state/form-value-cursor)}))

(defn- create-user-handler
  []
  (ajax/post
    (routes/path-for-backend :users)
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :user-info (select-keys response [:id]))))
                   (message/info
                     (str "User " (:id response) " has been created.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "User creation failed. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:valid?       false
                    :processing?  false
                    :showPassword false} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:username ""
                    :password ""
                    :email    ""
                    :role     "user"} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (init-form-value))))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [{:keys [username password role email]} (state/react state/form-value-cursor)
        {:keys [valid? processing? showPassword]} (state/react state/form-state-cursor)]
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
                  "Create user"))
              (comp/card-content
                {:className "Swarmpit-fcard-content"}
                (comp/typography
                  {:variant   "body2"
                   :className "Swarmpit-fcard-message"}
                  "Allow application access and management of the cluster")
                (form-username username)
                (form-password password showPassword)
                (form-email email)
                (form-role role))
              (comp/card-actions
                {:className "Swarmpit-fcard-actions"}
                (composite/progress-button
                  "Create"
                  create-user-handler
                  processing?))))]]))))