(ns swarmpit.component.dockerhub.create
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

(def cursor [:page :dockerhub :form])

(defn- form-username [value]
  (comp/form-comp
    "USERNAME"
    (comp/vtext-field
      {:name     "username"
       :key      "username"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:username] v cursor))})))

(defn- form-password [value]
  (comp/form-comp
    "PASSWORD"
    (comp/vtext-field
      {:name     "password"
       :key      "password"
       :type     "password"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:password] v cursor))})))

(defn- add-user-info-msg
  [id]
  (str "User " id " has been added."))

(defn- add-user-error-msg
  [error]
  (str "User cannot be added. Reason: " error))

(defn- add-user-handler
  []
  (ajax/POST (routes/path-for-backend :dockerhub-user-create)
             {:format        :json
              :headers       {"Authorization" (storage/get "token")}
              :params        (state/get-value cursor)
              :finally       (progress/mount!)
              :handler       (fn [response]
                               (let [id (get response "id")]
                                 (progress/unmount!)
                                 (dispatch!
                                   (routes/path-for-frontend :dockerhub-user-info {:id id}))
                                 (message/mount!
                                   (add-user-info-msg id))))
              :error-handler (fn [{:keys [response]}]
                               (let [error (get response "error")]
                                 (progress/unmount!)
                                 (message/mount!
                                   (add-user-error-msg error) true)))}))

(rum/defc form < rum/reactive []
  (let [{:keys [username
                password
                isValid]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/docker "New user")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Save"
            :disabled   (not isValid)
            :primary    true
            :onTouchTap add-user-handler}))]]
     [:div.form-edit
      (comp/form
        {:onValid   #(state/update-value [:isValid] true cursor)
         :onInvalid #(state/update-value [:isValid] false cursor)}
        (form-username username)
        (form-password password))]]))

(defn- init-state
  []
  (state/set-value {:username ""
                    :password ""
                    :isValid  false} cursor))

(defn mount!
  []
  (init-state)
  (rum/mount (form) (.getElementById js/document "content")))