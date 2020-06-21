(ns swarmpit.component.password
  (:require [material.components :as comp]
            [material.component.composite :as composite]
            [swarmpit.component.message :as message]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.common :as common]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn- form-password [password show-password?]
  (comp/text-field
    {:label        "Password"
     :fullWidth    true
     :name         "password"
     :key          "password"
     :variant      "outlined"
     :margin       "normal"
     :type         (if show-password?
                     "text"
                     "password")
     :defaultValue password
     :required     true
     :onChange     #(state/update-value [:password] (-> % .-target .-value) state/form-value-cursor)
     ;:InputLabelProps {:shrink true}
     :InputProps   {:endAdornment (common/show-password-adornment show-password?)}}))

(defn- form-new-password [password show-password?]
  (comp/text-field
    {:label        "New password"
     :fullWidth    true
     :name         "new-password"
     :key          "new-password"
     :variant      "outlined"
     :margin       "normal"
     :type         (if show-password?
                     "text"
                     "password")
     :defaultValue password
     :required     true
     :onChange     #(state/update-value [:new-password] (-> % .-target .-value) state/form-value-cursor)
     ;:InputLabelProps {:shrink true}
     :InputProps   {:endAdornment (common/show-password-adornment show-password?)}}))

(defn- form-confirm-password [confirm-password error? show-password?]
  (comp/text-field
    {:label        "Confirm password"
     :fullWidth    true
     :name         "confirm-password"
     :key          "confirm-password"
     :variant      "outlined"
     :error        error?
     :margin       "normal"
     :type         (if show-password?
                     "text"
                     "password")
     :defaultValue confirm-password
     :required     true
     :onChange     #(and (state/update-value [:confirm-password] (-> % .-target .-value) state/form-value-cursor)
                         (state/update-value [:error?] (not= (:new-password (state/get-value state/form-value-cursor)) (-> % .-target .-value)) state/form-state-cursor))
     ;:InputLabelProps {:shrink true}
     :InputProps   {:endAdornment (common/show-password-adornment show-password?)}}))

(defn- change-password-handler
  []
  (ajax/post
    (routes/path-for-backend :password)
    {:params     (state/get-value state/form-value-cursor)
     :on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :index))
                   (message/info
                     "Password has been changed"))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Can't update password: " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:error?       false
                    :processing?  false
                    :showPassword false} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:password         ""
                    :new-password     ""
                    :confirm-password ""} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (init-form-value))))

(rum/defc form < rum/reactive
                 mixin-init-form []
  (let [{:keys [password new-password confirm-password]} (state/react state/form-state-cursor)
        {:keys [error? processing? showPassword]} (state/react state/form-state-cursor)]
    (comp/paper
      {:className "Swarmpit-form-card"
       :elevation 0}
      (comp/card-content
        {}
        (form-password password showPassword)
        (form-new-password new-password showPassword)
        (form-confirm-password confirm-password error? showPassword))
      (comp/card-actions
        {:className "Swarmpit-fcard-actions"}
        (composite/progress-button
          "Change"
          change-password-handler
          processing?
          (or error? (some? password) (some? new-password)))))))