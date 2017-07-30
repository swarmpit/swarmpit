(ns swarmpit.component.password
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- input-password [local-state]
  (comp/form-comp
    "OLD PASSWORD"
    (comp/vtext-field
      {:name     "password"
       :key      "password"
       :required true
       :type     "password"
       :value    (:password @local-state)
       :onChange (fn [_ v]
                   (swap! local-state assoc :password v))})))

(defn- input-new-password [local-state]
  (comp/form-comp
    "NEW PASSWORD"
    (comp/vtext-field
      {:name            "new-password"
       :key             "new-password"
       :required        true
       :validations     "minLength:6"
       :validationError "Password must be at least 6 characters long"
       :type            "password"
       :value           (:new-password @local-state)
       :onChange        (fn [_ v]
                          (swap! local-state assoc :new-password v))})))

(defn- input-confirm-password [local-state]
  (comp/form-comp
    "CONFIRM PASSWORD"
    (comp/vtext-field
      {:name            "confirm-password"
       :key             "confirm-password"
       :required        true
       :validations     "equalsField:new-password"
       :validationError "Passwords does not match"
       :type            "password"
       :value           (:confirm-password @local-state)
       :onChange        (fn [_ v]
                          (swap! local-state assoc :confirm-password v))})))

(defn- change-password-handler
  [local-state]
  (handler/post
    (routes/path-for-backend :password)
    {:params     (dissoc @local-state :canSubmit)
     :on-success (fn [_]
                   (reset! local-state)
                   (dispatch!
                     (routes/path-for-frontend :index))
                   (message/info
                     "Password has been changed"))
     :on-error   (fn [response]
                   (message/error
                     (str "Can't update password: " (:error response))))}))

(rum/defcs form < (rum/local {:password         ""
                              :new-password     ""
                              :confirm-password ""
                              :canSubmit        false} ::password) [state]
  (let [local-state (::password state)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/password "New password")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Change"
            :disabled   (not (:canSubmit @local-state))
            :primary    true
            :onTouchTap #(change-password-handler local-state)}))]]
     [:div.form-edit
      (comp/form
        {:onValid   #(swap! local-state assoc :canSubmit true)
         :onInvalid #(swap! local-state assoc :canSubmit false)}
        (input-password local-state)
        (input-new-password local-state)
        (input-confirm-password local-state))]]))