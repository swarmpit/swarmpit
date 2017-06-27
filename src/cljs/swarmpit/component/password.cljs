(ns swarmpit.component.password
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- form-password [value local-state]
  (comp/form-comp
    "NEW PASSWORD"
    (comp/vtext-field
      {:name            "password"
       :key             "password"
       :required        true
       :validations     "minLength:6"
       :validationError "Password must be at least 6 characters long"
       :type            "password"
       :value           value
       :onChange        (fn [_ v]
                          (swap! local-state assoc :password v))})))

(defn- form-password-confirmation [value local-state]
  (comp/form-comp
    "CONFIRM PASSWORD"
    (comp/vtext-field
      {:name            "cpassword"
       :key             "cpassword"
       :required        true
       :validations     "equalsField:password"
       :validationError "Passwords does not match"
       :type            "password"
       :value           value
       :onChange        (fn [_ v]
                          (swap! local-state assoc :password2 v))})))

(defn- change-password-handler
  [local-state]
  (handler/post
    (routes/path-for-backend :password)
    (dissoc @local-state :canSubmit)
    (fn [_]
      (reset! local-state)
      (dispatch!
        (routes/path-for-frontend :index))
      (message/info
        "Password has been changed"))
    (fn [response]
      (message/error
        (str "Password update failed. Reason " (:error response))))))

(rum/defcs form < (rum/local {:password  ""
                              :password2 ""
                              :canSubmit false} ::password) [state]
  (let [local-state (::password state)
        password (:password @local-state)
        password2 (:password2 @local-state)
        canSubmit (:canSubmit @local-state)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/password "New password")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Change"
            :disabled   (not canSubmit)
            :primary    true
            :onTouchTap #(change-password-handler local-state)}))]]
     [:div.form-edit
      (comp/form
        {:onValid   #(swap! local-state assoc :canSubmit true)
         :onInvalid #(swap! local-state assoc :canSubmit false)}
        (form-password password local-state)
        (form-password-confirmation password2 local-state))]]))