(ns swarmpit.component.registry.create
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

(def cursor [:page :registry :form])

(defn- form-name [value]
  (comp/form-comp
    "NAME"
    (comp/text-field
      {:id       "name"
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:name] v cursor))})))

(defn- form-scheme [value]
  (comp/form-comp
    "SCHEME"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:scheme] v cursor))}
      (comp/menu-item
        {:key         "fshttp"
         :value       "http"
         :primaryText "HTTP"})
      (comp/menu-item
        {:key         "fshttps"
         :value       "https"
         :primaryText "HTTPS"}))))

(defn- form-url [value]
  (comp/form-comp
    "URL"
    (comp/text-field
      {:id       "url"
       :hintText "e.g. registry.hub.docker.com"
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:url] v cursor))})))

(defn- form-auth [value]
  (comp/form-comp
    "AUTHENTICATION"
    (comp/form-checkbox
      {:checked value
       :onCheck (fn [_ v]
                  (state/update-value [:withAuth] v cursor))})))

(defn- form-username [value]
  (comp/form-comp
    "USERNAME"
    (comp/text-field
      {:id       "username"
       :key      "username"
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:username] v cursor))})))

(defn- form-password [value]
  (comp/form-comp
    "PASSWORD"
    (comp/text-field
      {:id       "password"
       :key      "password"
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:password] v cursor))})))

(defn- create-registry-handler
  []
  (ajax/POST (routes/path-for-backend :registry-create)
             {:format        :json
              :headers       {"Authorization" (storage/get "token")}
              :params        (state/get-value cursor)
              :finally       (progress/mount!)
              :handler       (fn [response]
                               (let [id (get response "id")
                                     message (str "Registry " id " has been created.")]
                                 (progress/unmount!)
                                 (dispatch!
                                   (routes/path-for-frontend :registry-info {:id id}))
                                 (message/mount! message)))
              :error-handler (fn [{:keys [status response]}]
                               (let [error (get response "error")
                                     message (str "Registry creation failed. Status: " status " Reason: " error)]
                                 (print message)
                                 (progress/unmount!)
                                 (message/mount! error)))}))

(rum/defc form < rum/reactive []
  (let [{:keys [name
                scheme
                url
                withAuth
                username
                password]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/registries "New registry")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Create"
            :primary    true
            :onTouchTap create-registry-handler}))]]
     [:div.form-edit
      (form-name name)
      (form-scheme scheme)
      (form-url url)
      (form-auth withAuth)
      (if withAuth
        [:div
         (form-username username)
         (form-password password)])]]))

(defn- init-state
  []
  (state/set-value {:name     ""
                    :scheme   "https"
                    :url      ""
                    :withAuth false
                    :username ""
                    :password ""} cursor))

(defn mount!
  []
  (init-state)
  (rum/mount (form) (.getElementById js/document "content")))