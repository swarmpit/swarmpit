(ns swarmpit.component.page-login
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.component.state :as state]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.ajax :as ajax]
            [swarmpit.token :as token]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

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
    {:id         "loginUsername"
     :className  "SwarmpitFormLogin"
     :label      "Username"
     :key        "username-input"
     :required   true
     :value      value
     :onKeyPress (fn [event]
                   (on-enter event local-state))
     :onChange   (fn [event]
                   (swap! local-state assoc :username (-> event .-target .-value)))}))

(defn- form-password-adornment [local-state]
  (let [show-password? (:showPassword @local-state)]
    (comp/input-adornment
      {:position  "end"
       :className "SwarmpitFormLogin-password-adornment"}
      (comp/icon-button
        {:aria-label  "Toggle password visibility"
         :onClick     (fn []
                        (swap! local-state assoc :showPassword (not show-password?)))
         :onMouseDown (fn [event]
                        (.preventDefault event))}
        (if show-password?
          icon/visibility-off
          icon/visibility)))))

(defn- form-password [value local-state]
  (let [show-password? (:showPassword @local-state)]
    (comp/form-control
      {:className "SwarmpitFormLogin"
       :required  true}
      (comp/input-label
        {:htmlFor "adornment-password"
         :key     "password-input-label"} "Password")
      (comp/input
        {:id           "adornment-password"
         :label        "Password"
         :key          "password-input"
         :type         (if show-password?
                         "text"
                         "password")
         :value        value
         :onChange     (fn [event]
                         (swap! local-state assoc :password (-> event .-target .-value)))
         :onKeyPress   (fn [event]
                         (on-enter event local-state))
         :endAdornment (form-password-adornment local-state)}))))

(defn- form-button [local-state]
  (let [canSubmit? (:canSubmit @local-state)]
    (comp/button
      {:className "SwarmpitFormLogin-button"
       :disabled  (not canSubmit?)
       :variant   "outlined"
       :color     "secondary"
       :onClick   #(login-handler local-state)} "Login")))

(rum/defcs form < (rum/local {:username     ""
                              :password     ""
                              :message      ""
                              :canSubmit    true
                              :showPassword false} ::login) [state]
  (let [local-state (::login state)
        username (:username @local-state)
        password (:password @local-state)
        message (:message @local-state)]
    [:div.page-back
     [:div.page
      [:div.page-logo
       [:img {:src    "img/logo.png"
              :width  "350px"
              :height "350px"}]]
      [:div.page-form
       (comp/form-control
         {:required true
          :error    (not-empty message)}
         (when (not-empty message)
           (comp/form-helper-text {:error true} message))
         (comp/form-group
           {}
           (comp/mui (form-username username local-state))
           (comp/mui (form-password password local-state))
           (comp/mui (form-button local-state))))]]]))
