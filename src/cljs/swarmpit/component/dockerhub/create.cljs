(ns swarmpit.component.dockerhub.create
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- form-username [value]
  (form/comp
    "USERNAME"
    (comp/vtext-field
      {:name     "username"
       :key      "username"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:username] v state/form-value-cursor))})))

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
                   (state/update-value [:password] v state/form-value-cursor))})))

(defn- form-public [value]
  (form/comp
    "PUBLIC"
    (form/checkbox
      {:checked value
       :onCheck (fn [_ v]
                  (state/update-value [:public] v state/form-value-cursor))})))

(defn- add-user-handler
  []
  (ajax/post
    (routes/path-for-backend :dockerhub-user-create)
    {:params     (state/get-value state/form-value-cursor)
     :progress   [:processing?]
     :on-success (fn [response]
                   (dispatch!
                     (routes/path-for-frontend :dockerhub-user-info (select-keys response [:id])))
                   (message/info
                     (str "User " (:id response) " has been added.")))
     :on-error   (fn [response]
                   (message/error
                     (str "User cannot be added. Reason: " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:valid?      false
                    :processing? false} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:username ""
                    :password ""
                    :public   false} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (init-form-value))))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [{:keys [username password public]} (state/react state/form-value-cursor)
        {:keys [valid? processing?]} (state/react state/form-state-cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/docker "New user")]
      [:div.form-panel-right
       (comp/progress-button
         {:label      "Save"
          :disabled   (not valid?)
          :primary    true
          :onTouchTap add-user-handler} processing?)]]
     [:div.form-edit
      (form/form
        {:onValid   #(state/update-value [:valid?] true state/form-state-cursor)
         :onInvalid #(state/update-value [:valid?] false state/form-state-cursor)}
        (form-username username)
        (form-password password)
        (form-public public))]]))