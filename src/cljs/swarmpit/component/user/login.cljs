(ns swarmpit.component.user.login
  (:require [swarmpit.material :as material :refer [svg]]
            [rum.core :as rum]))

(defonce state (atom {:email    ""
                      :password ""}))

(defn- update-item
  "Update form item configuration"
  [k v]
  (swap! state assoc k v))

(defn- form-email [value]
  (material/theme
    (material/text-field
      #js {:id                "loginEmail"
           :floatingLabelText "Email"
           :value             value
           :onChange          (fn [e v] (update-item :email v))})))

(defn- form-password [value]
  (material/theme
    (material/text-field
      #js {:id                "loginPassword"
           :floatingLabelText "Password"
           :type              "password"
           :value             value
           :onChange          (fn [e v] (update-item :password v))})))

(defn- form-button []
  (material/theme
    (material/raised-button
      #js {:className "login-btn"
           :label     "Login"
           :primary   true})))

(rum/defc form < rum/reactive []
  (let [{:keys [email
                password]} (rum/react state)]
    [:div.login-back
     [:div.login
      (form-email email)
      (form-password password)
      (form-button)]]))

(defn mount!
  []
  (rum/mount (form) (.getElementById js/document "layout")))