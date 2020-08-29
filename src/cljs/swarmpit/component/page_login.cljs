(ns swarmpit.component.page-login
  (:require [material.icon :as icon]
            [material.components :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.common :as common]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.ajax :as ajax]
            [swarmpit.token :as token]
            [swarmpit.routes :as routes]
            [clojure.string :as str]
            [rum.core :as rum]
            [sablono.core :refer-macros [html]]
            [swarmpit.component.progress :as progress]))

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

(defn- create-admin-handler
  [local-state]
  (ajax/post
    (routes/path-for-backend :initialize)
    {:headers    {"Authorization" nil}
     :params     (select-keys @local-state [:username :password])
     :on-success (fn [_]
                   (swap! local-state assoc :message "")
                   (login-handler local-state)
                   (state/set-value true [:initialized]))
     :on-error   (fn [{:keys [response]}]
                   (swap! local-state assoc :message (:error response)))}))

(defn- on-enter
  [event local-state login?]
  (if (= 13 (.-charCode event))
    (if login?
      (login-handler local-state)
      (create-admin-handler local-state))))

(defn- form-username [value local-state]
  (comp/text-field
    {:id           "user"
     :key          "Swarmpit-login-username-input"
     :label        "Username"
     :variant      "outlined"
     :fullWidth    true
     :required     true
     :autoComplete "user"
     :autoFocus    true
     :defaultValue value
     :onChange     (fn [event]
                     (swap! local-state assoc :username (-> event .-target .-value)))}))

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
          (icon/visibility)
          (icon/visibility-off))))))

(defn- form-password [value error local-state login?]
  (let [show-password? (:showPassword @local-state)]
    (comp/text-field
      {:id           "password"
       :key          "Swarmpit-login-password-input"
       :label        "Password"
       :variant      "outlined"
       :error        (not (str/blank? error))
       :helperText   (when (not (str/blank? error)) error)
       :fullWidth    true
       :required     true
       :type         (if show-password?
                       "text"
                       "password")
       :defaultValue value
       :onChange     (fn [event]
                       (swap! local-state assoc :password (-> event .-target .-value)))
       :onKeyPress   (fn [event]
                       (on-enter event local-state login?))
       :InputProps   {:endAdornment (form-password-adornment local-state)}})))

(defn- form-login [local-state]
  (comp/button
    {:className "Swarmpit-login-form-submit"
     :type      "submit"
     :size      "large"
     :variant   "contained"
     :fullWidth true
     :color     "primary"
     :onClick   #(login-handler local-state)} "Sign in"))

(defn- form-create-admin [local-state]
  (comp/button
    {:className "Swarmpit-login-form-submit"
     :type      "submit"
     :variant   "contained"
     :fullWidth true
     :color     "primary"
     :onClick   #(create-admin-handler local-state)} "Create admin"))

(rum/defcs form < (rum/local {:username     ""
                              :password     ""
                              :message      ""
                              :showPassword false} ::login)
                  rum/reactive [state]
  (let [local-state (::login state)
        initialized (state/react [:initialized])
        version (state/react [:layout :version])
        username (:username @local-state)
        password (:password @local-state)
        message (:message @local-state)]
    [:div.Swarmpit-page
     [:div.Swarmpit-login-layout
      (comp/mui
        (comp/paper
          {:className "Swarmpit-login-paper"
           :elevation 0}
          (html
            [:img {:src   "img/logo.svg"
                   :class "Swarmpit-login-logo"
                   :width "220px"}])
          (html
            (progress/form
              (nil? initialized)
              {:height "200px"}
              (if initialized
                [:div.Swarmpit-login-form
                 (form-username username local-state)
                 (form-password password message local-state true)
                 (form-login local-state)]
                [:div.Swarmpit-login-form
                 [:p "Create first admin account and sign in."]
                 (form-username username local-state)
                 (form-password password message local-state false)
                 (form-create-admin local-state)])))))
      (common/title-login-version version)]]))