(ns swarmpit.component.page-login
  (:require [material.component :as comp]
            [material.icon :as icon]
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
  (comp/form-control
    {:margin    "normal"
     :required  true
     :fullWidth true}
    (comp/input-label
      {:htmlFor "user"} "User")
    (comp/input
      {:id           "user"
       :name         "user"
       :autoComplete "user"
       :autoFocus    true
       :value        value
       :onChange     (fn [event]
                       (swap! local-state assoc :username (-> event .-target .-value)))})))

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
          icon/visibility-off
          icon/visibility)))))

(defn- form-password [value local-state]
  (let [show-password? (:showPassword @local-state)]
    (comp/form-control
      {:className "SwarmpitFormLogin"
       :fullWidth true
       :required  true}
      (comp/input-label
        {:htmlFor "adornment-password"
         :key     "Swarmpit-login-password-input-label"} "Password")
      (comp/input
        {:id           "adornment-password"
         :label        "Password"
         :key          "Swarmpit-login-password-input"
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
      {:className "Swarmpit-login-form-submit"
       :disabled  (not canSubmit?)
       :type      "submit"
       :variant   "raised"
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
          (comp/avatar
            {:className "Swarmpit-login-avatar"
             :alt       "Swarmpit logo"
             :src       "/img/swarmpit-transparent.png"})

          ;(html
          ;  [:img {:src    "img/swarmpit.png"
          ;         :width  "80px"
          ;         :height "80px"}])

          (comp/typography
            {:variant   "headline"
             :className "Swarmpit-login-text"} "Welcome!")
          (html
            [:form.Swarmpit-login-form
             (when (not-empty message)
               (comp/form-helper-text {:error true} message))
             (form-username username local-state)
             (form-password password local-state)
             (form-button local-state)])))]]))
