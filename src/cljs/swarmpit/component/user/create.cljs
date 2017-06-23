(ns swarmpit.component.user.create
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(def cursor [:page :user :form])

(defn- form-username [value]
  (comp/form-comp
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
  (comp/form-comp
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
  (comp/form-comp
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
  (comp/form-comp
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

(defn- create-user-info-msg
  [id]
  (str "User " id " has been created."))

(defn- create-user-error-msg
  [error]
  (str "User creation failed. Reason: " error))

(defn- create-user-handler
  []
  (ajax/POST (routes/path-for-backend :user-create)
             {:format        :json
              :headers       {"Authorization" (storage/get "token")}
              :params        (state/get-value cursor)
              :finally       (progress/mount!)
              :handler       (fn [response]
                               (let [id (get response "id")]
                                 (progress/unmount!)
                                 (dispatch!
                                   (routes/path-for-frontend :user-info {:id id}))
                                 (message/mount!
                                   (create-user-info-msg id))))
              :error-handler (fn [{:keys [response]}]
                               (let [error (get response "error")]
                                 (progress/unmount!)
                                 (message/mount!
                                   (create-user-error-msg error) true)))}))

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
       (comp/panel-info icon/users "New user")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Create"
            :disabled   (not isValid)
            :primary    true
            :onTouchTap create-user-handler}))]]
     [:div.form-edit
      (comp/form
        {:onValid   #(state/update-value [:isValid] true cursor)
         :onInvalid #(state/update-value [:isValid] false cursor)}
        (form-username username)
        (form-password password)
        (form-role role)
        (form-email email))]]))