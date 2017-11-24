(ns swarmpit.component.page-login
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.event.source :as eventsource]
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
    {:headers    (login-headers local-state)
     :on-success (fn [response]
                   (reset! local-state)
                   (storage/add "token" (:token response))
                   (eventsource/init!)
                   (dispatch!
                     (routes/path-for-frontend :service-list)))
     :on-error   (fn [response]
                   (swap! local-state assoc :message (:error response)))}))

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

(defn- error-message
  [message]
  [:div
   [:svg.node-item-ico {:width  "24"
                        :height "24"
                        :fill   "rgb(117, 117, 117)"}
    [:path {:d icon/error}]]
   [:span message]])

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
      [:div.page-logo
       [:img {:src    "img/logo.svg"
              :width  "177px"
              :height "90px"}]]
      [:div.page-form
       (if (not-empty message)
         (error-message message))
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
            :onTouchTap #(login-handler local-state)}))]]]))