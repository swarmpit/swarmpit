(ns swarmpit.component.registry.create
  (:require [material.component :as comp]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(def cursor [:page :registry :form])

(def form-private-style
  {:marginTop "14px"})

(defn- form-name [value]
  (comp/form-comp
    "NAME"
    (comp/text-field
      {:id       "name"
       :value    value
       :onChange (fn [_ v]
                   (state/update-value :name v cursor))})))

(defn- form-version [value]
  (comp/form-comp
    "VERSION"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value :version v cursor))}
      (comp/menu-item
        {:key         "fv1"
         :value       "v1"
         :primaryText "v1"})
      (comp/menu-item
        {:key         "fv2"
         :value       "v2"
         :primaryText "v2"}))))

(defn- form-scheme [value]
  (comp/form-comp
    "SCHEME"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value :scheme v cursor))}
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
                   (state/update-value :url v cursor))})))

(defn- form-private [value]
  (comp/form-comp
    "IS PRIVATE"
    (comp/checkbox
      {:checked value
       :style   form-private-style
       :onCheck (fn [_ v]
                  (state/update-value :isPrivate v cursor))})))

(defn- form-user [value]
  (comp/form-comp
    "USER"
    (comp/text-field
      {:id       "user"
       :value    value
       :onChange (fn [_ v]
                   (state/update-value :user v cursor))})))

(defn- form-password [value]
  (comp/form-comp
    "PASSWORD"
    (comp/text-field
      {:id       "password"
       :value    value
       :onChange (fn [_ v]
                   (state/update-value :password v cursor))})))

(defn- create-registry-handler
  []
  (ajax/POST "/registries"
             {:format        :json
              :headers       {"Authorization" (storage/get "token")}
              :params        (state/get-value cursor)
              :finally       (progress/mount!)
              :handler       (fn [response]
                               (let [id (get response "Id")
                                     message (str "Registry " id " has been created.")]
                                 (progress/unmount!)
                                 (dispatch! (str "/#/registries"))
                                 (message/mount! message)))
              :error-handler (fn [{:keys [status response]}]
                               (let [error (get response "error")
                                     message (str "Registry creation failed. Status: " status " Reason: " error)]
                                 (print message)
                                 (progress/unmount!)
                                 (message/mount! message)))}))

(rum/defc form < rum/reactive []
  (let [{:keys [name
                version
                scheme
                url
                isPrivate
                user
                password]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Create"
            :primary    true
            :onTouchTap create-registry-handler}))]]
     [:div.form-edit
      (form-name name)
      (form-version version)
      (form-scheme scheme)
      (form-url url)
      (form-private isPrivate)
      (form-user user)
      (form-password password)]]))

(defn- init-state
  []
  (state/set-value {:name      ""
                    :version   "v2"
                    :scheme    "http"
                    :url       ""
                    :isPrivate false
                    :user      ""
                    :password  ""} cursor))

(defn mount!
  []
  (init-state)
  (rum/mount (form) (.getElementById js/document "content")))