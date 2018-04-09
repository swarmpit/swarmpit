(ns swarmpit.component.registry.create
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

(defn- form-name [value]
  (form/comp
    "NAME"
    (comp/vtext-field
      {:name     "name"
       :key      "name"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:name] v state/form-value-cursor))})))

(defn- form-url [value]
  (form/comp
    "URL"
    (comp/vtext-field
      {:name            "url"
       :key             "url"
       :required        true
       :validations     "isUrl"
       :validationError "Please provide a valid URL"
       :hintText        "e.g. https://my.registry.io"
       :value           value
       :onChange        (fn [_ v]
                          (state/update-value [:url] v state/form-value-cursor))})))

(defn- form-auth [value]
  (form/comp
    "AUTHENTICATION"
    (form/checkbox
      {:key     "authentication"
       :checked value
       :onCheck (fn [_ v]
                  (state/update-value [:withAuth] v state/form-value-cursor))})))

(defn- form-username [value]
  (form/comp
    "USERNAME"
    (comp/text-field
      {:name     "username"
       :key      "username"
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:username] v state/form-value-cursor))})))

(defn- form-password [value]
  (form/comp
    "PASSWORD"
    (comp/text-field
      {:name     "password"
       :key      "password"
       :type     "password"
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

(defn- create-registry-handler
  []
  (ajax/post
    (routes/path-for-backend :registry-create)
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :registry-info (select-keys response [:id]))))
                   (message/info
                     (str "Registry " (:id response) " has been created.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Registry creation failed. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:valid?      false
                    :processing? false} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:name     ""
                    :url      ""
                    :public   false
                    :withAuth false
                    :username ""
                    :password ""} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (init-form-value))))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [{:keys [name url public withAuth username password]} (state/react state/form-value-cursor)
        {:keys [valid? processing?]} (state/react state/form-state-cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/registries "Add registry")]
      [:div.form-panel-right
       (comp/progress-button
         {:label      "Save"
          :disabled   (not valid?)
          :primary    true
          :onTouchTap create-registry-handler} processing?)]]
     [:div.form-edit
      (form/form
        {:onValid   #(state/update-value [:valid?] true state/form-state-cursor)
         :onInvalid #(state/update-value [:valid?] false state/form-state-cursor)}
        (form-name name)
        (form-url url)
        (form-public public)
        (form-auth withAuth)
        (if withAuth
          (form/comps
            (form-username username)
            (form-password password))))]]))