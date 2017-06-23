(ns swarmpit.component.dockerhub.create
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

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

(defn- add-user-handler
  []
  (handler/post
    (routes/path-for-backend :dockerhub-user-create)
    (state/get-value cursor)
    (fn [response]
      (dispatch!
        (routes/path-for-frontend :dockerhub-user-info (select-keys response [:id])))
      (state/set-value {:text (str "User " (:id response) " has been added.")
                        :type :info
                        :open true} message/cursor))
    (fn [response]
      (state/set-value {:text (str "User cannot be added. Reason: " (:error response))
                        :type :error
                        :open true} message/cursor))))

(defn- init-state
  []
  (state/set-value {:username ""
                    :password ""
                    :isValid  false} cursor))

(def init-state-mixin
  (mixin/init
    (fn [_]
      (init-state))))

(rum/defc form < rum/reactive
                 init-state-mixin []
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