(ns swarmpit.component.password
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(defonce state (atom {:password  ""
                      :password2 ""
                      :canSubmit false}))

(defn- update-item
  "Update form item configuration"
  [k v]
  (swap! state assoc k v))

(defn- form-password [value]
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
                          (update-item :password v))})))

(defn- form-password-confirmation [value]
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
                          (update-item :password2 v))})))

(defn- change-password-error-msg
  [error]
  (str "Password update failed. Reason " error))

(defn- change-password-handler
  []
  (ajax/POST (routes/path-for-backend :password)
             {:format        :json
              :headers       {"Authorization" (storage/get "token")}
              :params        (dissoc @state :canSubmit)
              :handler       (fn [_]
                               (dispatch!
                                 (routes/path-for-frontend :index))
                               (message/mount! "Password has been changed"))
              :error-handler (fn [{:keys [response]}]
                               (let [error (get response "error")]
                                 (message/mount!
                                   (change-password-error-msg error) true)))}))

(rum/defc form < rum/reactive []
  (let [{:keys [password
                password2
                canSubmit]} (rum/react state)]
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
            :onTouchTap change-password-handler}))]]
     [:div.form-edit
      (comp/form
        {:onValid   #(update-item :canSubmit true)
         :onInvalid #(update-item :canSubmit false)}
        (form-password password)
        (form-password-confirmation password2))]]))