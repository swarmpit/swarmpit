(ns swarmpit.component.user.create
  (:require [material.components :as comp]
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
     :margin          "normal"
     :defaultValue    value
     :required        true
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:username] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-password [value show-password?]
  (comp/text-field
    {:label           "Password"
     :variant         "outlined"
     :key             "password"
     :fullWidth       true
     :required        true
     :margin          "normal"
     :type            (if show-password?
                        "text"
                        "password")
     :defaultValue    value
     :onChange        #(state/update-value [:password] (-> % .-target .-value) state/form-value-cursor)
     :InputLabelProps {:shrink true}
     :InputProps      {:endAdornment (common/show-password-adornment show-password?)}}))

(defn- form-role [value]
  (comp/form-control
    {:component "fieldset"
     :key       "role-f"
     :margin    "normal"
     :style     {:width "200px"}}
    (comp/form-label
      {:key "rolel"} "Role")
    (comp/radio-group
      {:name     "role"
       :key      "role-rg"
       :value    value
       :onChange #(state/update-value [:role] (-> % .-target .-value) state/form-value-cursor)}
      (comp/form-control-label
        {:control (comp/radio
                    {:name  "admin-role"
                     :color "primary"
                     :key   "admin-role"})
         :key     "ad-role"
         :value   "admin"
         :label   "Admin"})
      (comp/form-control-label
        {:control (comp/radio
                    {:name  "user-role"
                     :color "primary"
                     :key   "user-role"})
         :key     "usr-role"
         :value   "user"
         :label   "User"}))))

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
    (routes/path-for-backend :user-create)
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
          (comp/grid
            {:item true
             :xs   12
             :sm   6
             :md   4}
            (comp/card
              {:className "Swarmpit-form-card"
               :key       "ucc"}
              (comp/card-header
                {:className "Swarmpit-form-card-header"
                 :key       "ucch"
                 :title     "New User"})
              (comp/card-content
                {:key "uccc"}
                (comp/grid
                  {:container true
                   :key       "ucccc"
                   :spacing   40}
                  (comp/grid
                    {:item true
                     :key  "uccccig"
                     :xs   12}
                    (form-username username)
                    (form-password password showPassword)
                    (form-role role)
                    (form-email email)))
                (html
                  [:div {:class "Swarmpit-form-buttons"
                         :key   "ucccbtn"}
                   (composite/progress-button
                     "Create"
                     create-user-handler
                     processing?)]))))]]))))
