(ns swarmpit.component.page-login
  (:require [material.icon :as icon]
            [material.components :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.ajax :as ajax]
            [swarmpit.token :as token]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [sablono.core :refer-macros [html]]))

(enable-console-print!)

(defn- login-headers
  [local-state]
  (let [token (token/generate-basic (:username @local-state)
                                    (:password @local-state))]
    {"Authorization" token}))

(defn- login-handler
  [local-state]
  (ajax/post
    (routes/path-for-backend :login)
    {:headers    (login-headers local-state)
     :on-success (fn [{:keys [response]}]
                   (reset! local-state nil)
                   (storage/add "token" (:token response))
                   (let [redirect-location (state/get-value [:redirect-location])]
                     (state/set-value nil [:redirect-location])
                     (dispatch!
                       (or redirect-location (routes/path-for-frontend :service-list)))))
     :on-error   (fn [{:keys [response]}]
                   (swap! local-state assoc :message (:error response)))}))

(defn- on-enter
  [event local-state]
  (if (= 13 (.-charCode event))
    (login-handler local-state)))

(defn- form-username [value local-state]
  (comp/text-field
    {:id              "user"
     :key             "Swarmpit-login-username-input"
     :label           "Username"
     :variant         "outlined"
     :fullWidth       true
     :required        true
     :autoComplete    "user"
     :autoFocus       true
     :defaultValue    value
     :onChange        (fn [event]
                        (swap! local-state assoc :username (-> event .-target .-value)))
     :InputLabelProps {:shrink true}}))

(defn- form-password-adornment [local-state]
  (let [show-password? (:showPassword @local-state)]
    (comp/input-adornment
      {:position "end"}
      (comp/icon-button
        {:aria-label  "Toggle password visibility"
         :onClick     (fn []
                        (swap! local-state assoc :showPassword (not show-password?)))
         :onMouseDown (fn [event]
                        (.preventDefault event))}
        (if show-password?
          icon/visibility
          icon/visibility-off)))))

(defn- form-password [value local-state]
  (let [show-password? (:showPassword @local-state)]
    (comp/text-field
      {:id              "password"
       :key             "Swarmpit-login-password-input"
       :label           "Password"
       :variant         "outlined"
       :fullWidth       true
       :required        true
       :type            (if show-password?
                          "text"
                          "password")
       :defaultValue    value
       :onChange        (fn [event]
                          (swap! local-state assoc :password (-> event .-target .-value)))
       :onKeyPress      (fn [event]
                          (on-enter event local-state))
       :InputLabelProps {:shrink true}
       :InputProps      {:endAdornment (form-password-adornment local-state)}})))

(defn- form-button [local-state]
  (let [canSubmit? (:canSubmit @local-state)]
    (comp/button
      {:className "Swarmpit-login-form-submit"
       :disabled  (not canSubmit?)
       :type      "submit"
       :variant   "contained"
       :fullWidth true
       :color     "primary"
       :onClick   #(login-handler local-state)} "Sign in")))

(rum/defcs form < (rum/local {:username     ""
                              :password     ""
                              :message      ""
                              :canSubmit    true
                              :showPassword false} ::login) [state]
  (let [local-state (::login state)
        username (:username @local-state)
        password (:password @local-state)
        message (:message @local-state)]
    [:div.Swarmpit-page
     [:div.Swarmpit-login-layout
      (comp/mui
        (comp/paper
          {:className "Swarmpit-login-paper"}
          (html
            [:img {:src    "img/swarmpit.png"
                   :key    "Swarmpit-login-img"
                   :width  "100%"
                   :height "100%"}])
          (when (not-empty message)
            (html
              [:span {:key   "Swarmpit-login-mssg"
                      :style {:display    "flex"
                              :alignItems "center"
                              :color      "#d32f2f"
                              :padding    20}}
               (icon/error {}) message]))
          (html
            [:div.Swarmpit-login-form {:key "Swarmpit-login-form"}
             (form-username username local-state)
             (form-password password local-state)
             (form-button local-state)])))]]))
