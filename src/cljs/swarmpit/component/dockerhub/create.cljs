(ns swarmpit.component.dockerhub.create
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

(def cursor [:form])

(defonce valid? (atom false))

(defn- form-username [value]
  (form/comp
    "USERNAME"
    (comp/vtext-field
      {:name     "username"
       :key      "username"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:username] v cursor))})))

(defn- form-password [value]
  (form/comp
    "PASSWORD"
    (comp/vtext-field
      {:name     "password"
       :key      "password"
       :type     "password"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:password] v cursor))})))

(defn- form-public [value]
  (form/comp
    "PUBLIC"
    (form/checkbox
      {:checked value
       :onCheck (fn [_ v]
                  (state/update-value [:public] v cursor))})))

(defn- add-user-handler
  []
  (handler/post
    (routes/path-for-backend :dockerhub-user-create)
    {:params     (state/get-value cursor)
     :on-success (fn [response]
                   (dispatch!
                     (routes/path-for-frontend :dockerhub-user-info (select-keys response [:id])))
                   (message/info
                     (str "User " (:id response) " has been added.")))
     :on-error   (fn [response]
                   (message/error
                     (str "User cannot be added. Reason: " (:error response))))}))

(defn- init-state
  []
  (state/set-value {:username ""
                    :password ""
                    :public   false} cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-state))))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [{:keys [username
                password
                public]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/docker "New user")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Save"
            :disabled   (not (rum/react valid?))
            :primary    true
            :onTouchTap add-user-handler}))]]
     [:div.form-edit
      (form/form
        {:onValid   #(reset! valid? true)
         :onInvalid #(reset! valid? false)}
        (form-username username)
        (form-password password)
        (form-public public))]]))