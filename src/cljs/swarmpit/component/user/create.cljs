(ns swarmpit.component.user.create
  (:require [material.icon :as icon]
            [material.components :as comp]
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
     :value           value
     :required        true
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:username] (-> % .-target .-value) state/form-value-cursor)}))

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
     :InputProps      {:endAdornment (common/show-password-adornment show-password?)}}))

(defn- form-role [value]
  (comp/text-field
    {:fullWidth       true
     :label           "Role"
     :select          true
     :value           value
     :variant         "outlined"
     :margin          "normal"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:role] (-> % .-target .-value) state/form-value-cursor)}
    (comp/menu-item
      {:key   "admin"
       :value "admin"} "admin")
    (comp/menu-item
      {:key   "user"
       :value "user"} "user")))

(defn- form-email [value]
  (comp/text-field
    {:label           "Email"
     :fullWidth       true
     :variant         "outlined"
     :value           value
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
  (let [{:keys [username password role email]} (state/react state/form-state-cursor)
        {:keys [valid? processing? showPassword]} (state/react state/form-state-cursor)]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (comp/grid
            {:item true
             :xs   12
             :sm   6
             :md   3}
            (comp/card
              {:className "Swarmpit-form-card"}
              (comp/card-header
                {:className "Swarmpit-form-card-header"
                 :title     "New User"})
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
                    (form-role role)
                    (form-email email)))
                (html
                  [:div.Swarmpit-form-buttons
                   (comp/button
                     {:variant "contained"
                      :onClick create-user-handler
                      :color   "primary"} "Create")]))))]]))))
