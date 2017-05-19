(ns swarmpit.component.page-login
  (:require [swarmpit.uri :refer [dispatch!]]
            [swarmpit.material :as material]
            [swarmpit.storage :as storage]
            [swarmpit.token :as token]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(defonce state (atom {:email    ""
                      :password ""
                      :message  ""}))

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

(defn- login-headers
  []
  (let [token (token/generate-basic (:email @state)
                                    (:password @state))]
    {"Authorization" token}))

(defn- login-handler
  []
  (ajax/POST "/login"
             {:format        :json
              :headers       (login-headers)
              :handler       (fn [response]
                               (let [token (get response "token")]
                                 (storage/add "token" token)
                                 (dispatch! "/")))
              :error-handler (fn [{:keys [response]}]
                               (let [error (get response "error")]
                                 (update-item :message error)))}))

(rum/defc form < rum/reactive []
  (let [{:keys [email
                password
                message]} (rum/react state)]
    [:div.login-back
     [:div.login
      [:div.message message]
      (form-email email)
      (form-password password)
      (material/theme
        (material/raised-button
          #js {:className  "login-btn"
               :label      "Login"
               :primary    true
               :onTouchTap login-handler}))]]))

(defn mount!
  []
  (rum/mount (form) (.getElementById js/document "layout")))