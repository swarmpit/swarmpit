(ns swarmpit.component.page-login
  (:require [material.component :as comp]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.token :as token]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(defonce state (atom {:username  ""
                      :password  ""
                      :message   ""
                      :canSubmit false}))

(def login-button-style
  {:marginTop "30px"})

(defn- update-item
  "Update form item configuration"
  [k v]
  (swap! state assoc k v))

(defn- form-username [value]
  (comp/vtext-field
    {:id                "loginUsername"
     :key               "username"
     :name              "username"
     :required          true
     :floatingLabelText "Username"
     :value             value
     :onChange          (fn [_ v] (update-item :username v))}))

(defn- form-password [value]
  (comp/vtext-field
    {:id                "loginPassword"
     :key               "password"
     :name              "password"
     :required          true
     :floatingLabelText "Password"
     :type              "password"
     :value             value
     :onChange          (fn [_ v] (update-item :password v))}))

(defn- login-headers
  []
  (let [token (token/generate-basic (:username @state)
                                    (:password @state))]
    {"Authorization" token}))

(defn- login-handler
  []
  (ajax/POST (routes/path-for-backend :login)
             {:format        :json
              :headers       (login-headers)
              :handler       (fn [response]
                               (let [token (get response "token")]
                                 (storage/add "token" token)
                                 (dispatch!
                                   (routes/path-for-frontend :index))))
              :error-handler (fn [{:keys [response]}]
                               (let [error (get response "error")]
                                 (update-item :message error)))}))

(rum/defc form < rum/reactive []
  (let [{:keys [username
                password
                message
                canSubmit]} (rum/react state)]
    [:div.page-back
     [:div.page
      [:div message]
      (comp/mui
        (comp/vform
          {:onValid   #(update-item :canSubmit true)
           :onInvalid #(update-item :canSubmit false)}
          (form-username username)
          (form-password password)))
      (comp/mui
        (comp/raised-button
          {:style      login-button-style
           :disabled   (not canSubmit)
           :label      "Login"
           :primary    true
           :onTouchTap login-handler}))]]))

(defn mount!
  []
  (rum/mount (form) (.getElementById js/document "layout")))