(ns swarmpit.component.registry.create
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

(def cursor [:page :registry :form])

(defn- form-name [value]
  (comp/form-comp
    "NAME"
    (comp/vtext-field
      {:name     "name"
       :key      "name"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:name] v cursor))})))

(defn- form-url [value]
  (comp/form-comp
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
  (comp/form-comp
    "AUTHENTICATION"
    (comp/form-checkbox
      {:key     "authentication"
       :checked value
       :onCheck (fn [_ v]
                  (state/update-value [:withAuth] v cursor))})))

(defn- form-username [value]
  (comp/form-comp
    "USERNAME"
    (comp/text-field
      {:name     "username"
       :key      "username"
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:username] v cursor))})))

(defn- form-password [value]
  (comp/form-comp
    "PASSWORD"
    (comp/text-field
      {:name     "password"
       :key      "password"
       :type     "password"
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:password] v cursor))})))

(defn- create-registry-handler
  []
  (handler/post
    (routes/path-for-backend :registry-create)
    (state/get-value cursor)
    (fn [response]
      (dispatch!
        (routes/path-for-frontend :registry-info (select-keys response [:id])))
      (state/set-value {:text (str "Registry " (:id response) " has been created.")
                        :type :info
                        :open true} message/cursor))
    (fn [response]
      (state/set-value {:text (str "Registry creation failed. Reason: " (:error response))
                        :type :error
                        :open true} message/cursor))))

(defn- init-state
  []
  (state/set-value {:name     ""
                    :url      ""
                    :withAuth false
                    :username ""
                    :password ""
                    :isValid  false} cursor))

(def init-state-mixin
  (mixin/init
    (fn [_]
      (init-state))))

(rum/defc form < rum/reactive
                 init-state-mixin []
  (let [{:keys [name
                url
                withAuth
                username
                password
                isValid]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/registries "New registry")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Save"
            :disabled   (not isValid)
            :primary    true
            :onTouchTap create-registry-handler}))]]
     [:div.form-edit
      (comp/form
        {:onValid   #(state/update-value [:isValid] true cursor)
         :onInvalid #(state/update-value [:isValid] false cursor)}
        (form-name name)
        (form-url url)
        (form-auth withAuth)
        (if withAuth
          (comp/form-comps
            (form-username username)
            (form-password password))))]]))