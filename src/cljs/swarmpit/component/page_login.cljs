(ns swarmpit.component.page-login
  (:require [material.component :as comp]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.handler :as handler]
            [swarmpit.token :as token]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def login-button-style
  {:marginTop "30px"})

(defn- login-headers
  [local-state]
  (let [token (token/generate-basic (:username @local-state)
                                    (:password @local-state))]
    {"Authorization" token}))

(defn- login-handler
  [local-state]
  (handler/post
    (routes/path-for-backend :login)
    {}
    (login-headers local-state)
    (fn [response]
      (reset! local-state)
      (storage/add "token" (:token response))
      (dispatch!
        (routes/path-for-frontend :index)))
    (fn [response]
      (swap! local-state assoc :message (:error response)))))

(defn- on-enter
  [event local-state]
  (if (= 13 (.-charCode event))
    (login-handler local-state)))

(defn- form-username [value local-state]
  (comp/vtext-field
    {:id                "loginUsername"
     :key               "username"
     :name              "username"
     :required          true
     :floatingLabelText "Username"
     :value             value
     :onKeyPress        (fn [event]
                          (on-enter event local-state))
     :onChange          (fn [_ v]
                          (swap! local-state assoc :username v))}))

(defn- form-password [value local-state]
  (comp/vtext-field
    {:id                "loginPassword"
     :key               "password"
     :name              "password"
     :required          true
     :floatingLabelText "Password"
     :type              "password"
     :value             value
     :onKeyPress        (fn [event]
                          (on-enter event local-state))
     :onChange          (fn [_ v]
                          (swap! local-state assoc :password v))}))

(rum/defcs form < (rum/local {:username  ""
                              :password  ""
                              :message   ""
                              :canSubmit false} ::login) [state]
  (let [local-state (::login state)
        username (:username @local-state)
        password (:password @local-state)
        message (:message @local-state)
        canSubmit (:canSubmit @local-state)]
    [:div.page-back
     [:div.page
      [:div message]
      (comp/mui
        (comp/vform
          {:onValid   #(swap! local-state assoc :canSubmit true)
           :onInvalid #(swap! local-state assoc :canSubmit false)}
          (form-username username local-state)
          (form-password password local-state)))
      (comp/mui
        (comp/raised-button
          {:style      login-button-style
           :disabled   (not canSubmit)
           :label      "Login"
           :primary    true
           :onTouchTap #(login-handler local-state)}))]]))