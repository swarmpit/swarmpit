(ns swarmpit.component.user.create
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :user :form])

(defn- form-username [value]
  (form/comp
    "USERNAME"
    (comp/vtext-field
      {:name            "username"
       :key             "username"
       :required        true
       :validations     "minLength:4"
       :validationError "Username must be at least 4 characters long"
       :value           value
       :onChange        (fn [_ v]
                          (state/update-value [:username] v cursor))})))

(defn- form-password [value]
  (form/comp
    "PASSWORD"
    (comp/vtext-field
      {:name            "password"
       :key             "password"
       :required        true
       :validations     "minLength:6"
       :validationError "Password must be at least 6 characters long"
       :type            "password"
       :value           value
       :onChange        (fn [_ v]
                          (state/update-value [:password] v cursor))})))

(defn- form-role [value]
  (form/comp
    "ROLE"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:role] v cursor))}
      (comp/menu-item
        {:key         "fru"
         :value       "admin"
         :primaryText "admin"})
      (comp/menu-item
        {:key         "fra"
         :value       "user"
         :primaryText "user"}))))

(defn- form-email [value]
  (form/comp
    "EMAIL"
    (comp/vtext-field
      {:name            "email"
       :key             "email"
       :required        true
       :validations     "isEmail"
       :validationError "Please provide a valid Email"
       :value           value
       :onChange        (fn [_ v]
                          (state/update-value [:email] v cursor))})))

(defn- create-user-handler
  []
  (handler/post
    (routes/path-for-backend :user-create)
    {:params     (state/get-value cursor)
     :on-success (fn [response]
                   (dispatch!
                     (routes/path-for-frontend :user-info (select-keys response [:id])))
                   (message/info
                     (str "User " (:id response) " has been created.")))
     :on-error   (fn [response]
                   (message/error
                     (str "User creation failed. Reason: " (:error response))))}))

(defn- init-state
  []
  (state/set-value {:username ""
                    :password ""
                    :email    ""
                    :role     "user"
                    :isValid  false} cursor))

(def init-state-mixin
  (mixin/init
    (fn [_]
      (init-state))))

(rum/defc form < rum/reactive
                 init-state-mixin []
  (let [{:keys [username
                password
                role
                email
                isValid]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/users "New user")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Create"
            :disabled   (not isValid)
            :primary    true
            :onTouchTap create-user-handler}))]]
     [:div.form-edit
      (form/form
        {:onValid   #(state/update-value [:isValid] true cursor)
         :onInvalid #(state/update-value [:isValid] false cursor)}
        (form-username username)
        (form-password password)
        (form-role role)
        (form-email email))]]))