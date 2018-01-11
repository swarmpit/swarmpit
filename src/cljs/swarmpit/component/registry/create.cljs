(ns swarmpit.component.registry.create
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

(defn- form-name [value]
  (form/comp
    "NAME"
    (comp/vtext-field
      {:name     "name"
       :key      "name"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:name] v cursor))})))

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
                          (state/update-value [:url] v cursor))})))

(defn- form-auth [value]
  (form/comp
    "AUTHENTICATION"
    (form/checkbox
      {:key     "authentication"
       :checked value
       :onCheck (fn [_ v]
                  (state/update-value [:withAuth] v cursor))})))

(defn- form-username [value]
  (form/comp
    "USERNAME"
    (comp/text-field
      {:name     "username"
       :key      "username"
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:username] v cursor))})))

(defn- form-password [value]
  (form/comp
    "PASSWORD"
    (comp/text-field
      {:name     "password"
       :key      "password"
       :type     "password"
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

(defn- create-registry-handler
  []
  (handler/post
    (routes/path-for-backend :registry-create)
    {:params     (state/get-value cursor)
     :on-success (fn [response]
                   (dispatch!
                     (routes/path-for-frontend :registry-info (select-keys response [:id])))
                   (message/info
                     (str "Registry " (:id response) " has been created.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Registry creation failed. Reason: " (:error response))))}))

(defn- init-state
  []
  (state/set-value {:name     ""
                    :url      ""
                    :public   false
                    :withAuth false
                    :username ""
                    :password ""} cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-state))))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [{:keys [name
                url
                public
                withAuth
                username
                password]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/registries "New registry")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Save"
            :disabled   (not (rum/react valid?))
            :primary    true
            :onTouchTap create-registry-handler}))]]
     [:div.form-edit
      (form/form
        {:onValid   #(reset! valid? true)
         :onInvalid #(reset! valid? false)}
        (form-name name)
        (form-url url)
        (form-public public)
        (form-auth withAuth)
        (if withAuth
          (form/comps
            (form-username username)
            (form-password password))))]]))