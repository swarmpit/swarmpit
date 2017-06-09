(ns swarmpit.component.user.create
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
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
    (comp/text-field
      {:id       "username"
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:username] v cursor))})))

(defn- form-password [value]
  (comp/form-comp
    "URL"
    (comp/text-field
      {:id       "password"
       :value    value
       :onChange (fn [_ v]
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
    (comp/text-field
      {:id       "email"
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:email] v cursor))})))

(defn- create-user-handler
  []
  (ajax/POST (routes/path-for-backend :user-create)
             {:format        :json
              :headers       {"Authorization" (storage/get "token")}
              :params        (state/get-value cursor)
              :finally       (progress/mount!)
              :handler       (fn [response]
                               (let [id (get response "id")
                                     message (str "User " id " has been created.")]
                                 (progress/unmount!)
                                 (dispatch!
                                   (routes/path-for-frontend :user-info {:id id}))
                                 (message/mount! message)))
              :error-handler (fn [{:keys [status response]}]
                               (let [error (get response "error")
                                     message (str "User creation failed. Status: " status " Reason: " error)]
                                 (print message)
                                 (progress/unmount!)
                                 (message/mount! error)))}))

(rum/defc form < rum/reactive []
  (let [{:keys [username
                password
                role
                email]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/users "New user")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Create"
            :primary    true
            :onTouchTap create-user-handler}))]]
     [:div.form-edit
      (form-username username)
      (form-password password)
      (form-role role)
      (form-email email)]]))

(defn- init-state
  []
  (state/set-value {:username ""
                    :password ""
                    :email    ""
                    :role     "user"} cursor))

(defn mount!
  []
  (init-state)
  (rum/mount (form) (.getElementById js/document "content")))