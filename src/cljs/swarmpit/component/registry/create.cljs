(ns swarmpit.component.registry.create
  (:require [material.component :as comp]
            [swarmpit.uri :refer [dispatch!]]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
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
                   (state/update-value :name v cursor))})))

(defn- form-url [value]
  (comp/form-comp
    "URL"
    (comp/text-field
      {:id       "url"
       :value    value
       :onChange (fn [_ v]
                   (state/update-value :url v cursor))})))

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
              :params        (state/get-value cursor)
              :finally       (progress/mount!)
              :handler       (fn [response]
                               (let [id (get response "Id")
                                     message (str "Registry " id " has been created.")]
                                 (progress/unmount!)
                                 (dispatch! (str "/#/networks/" id))
                                 (message/mount! message)))
              :error-handler (fn [{:keys [status status-text]}]
                               (print "d")
                               (let [message (str "Registry creation failed. Status: " status " Reason: " status-text)]
                                 (progress/unmount!)
                                 (message/mount! message)))}))

(rum/defc form < rum/reactive []
  (let [{:keys [name
                url
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
      (form-url url)
      (form-user user)
      (form-password password)]]))

(defn- init-state
  []
  (state/set-value {:name     ""
                    :url      ""
                    :user     ""
                    :password ""} cursor))

(defn mount!
  []
  (init-state)
  (rum/mount (form) (.getElementById js/document "content")))