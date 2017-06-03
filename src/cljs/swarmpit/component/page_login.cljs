(ns swarmpit.component.page-login
  (:require [material.component :as comp]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.token :as token]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(defonce state (atom {:email    ""
                      :password ""
                      :message  ""}))

(def login-button-style
  {:marginTop "30px"})

(defn- update-item
  "Update form item configuration"
  [k v]
  (swap! state assoc k v))

(defn- form-email [value]
  (comp/mui
    (comp/text-field
      {:id                "loginEmail"
       :floatingLabelText "Email"
       :value             value
       :onChange          (fn [e v] (update-item :email v))})))

(defn- form-password [value]
  (comp/mui
    (comp/text-field
      {:id                "loginPassword"
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
    [:div.page-back
     [:div.page
      [:div message]
      (form-email email)
      (form-password password)
      (comp/mui
        (comp/raised-button
          {:style      login-button-style
           :label      "Login"
           :primary    true
           :onTouchTap login-handler}))]]))

(defn mount!
  []
  (rum/mount (form) (.getElementById js/document "layout")))