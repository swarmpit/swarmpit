(ns swarmpit.component.network.create
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

(def cursor [:page :network :form])

(defn- form-name [value]
  (comp/form-comp
    "NAME"
    (comp/vtext-field
      {:name     "name"
       :key      "name"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:networkName] v cursor))})))

(defn- form-driver [value]
  (comp/form-comp
    "DRIVER"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:driver] v cursor))}
      (comp/menu-item
        {:key         "fdi1"
         :value       "overlay"
         :primaryText "overlay"})
      (comp/menu-item
        {:key         "fdi3"
         :value       "bridge"
         :primaryText "bridge"}))))

(defn- form-internal [value]
  (comp/form-comp
    "IS PRIVATE"
    (comp/form-checkbox
      {:checked value
       :onCheck (fn [_ v]
                  (state/update-value [:internal] v cursor))})))

(defn- create-network-handler
  []
  (ajax/POST (routes/path-for-backend :network-create)
             {:format        :json
              :headers       {"Authorization" (storage/get "token")}
              :params        (state/get-value cursor)
              :finally       (progress/mount!)
              :handler       (fn [response]
                               (let [id (get response "Id")
                                     message (str "Network " id " has been created.")]
                                 (progress/unmount!)
                                 (dispatch!
                                   (routes/path-for-frontend :network-info {:id id}))
                                 (message/mount! message)))
              :error-handler (fn [{:keys [status response]}]
                               (let [error (get response "error")
                                     message (str "Network creation failed. Status: " status " Reason: " error)]
                                 (progress/unmount!)
                                 (message/mount! message)))}))

(rum/defc form < rum/reactive []
  (let [{:keys [name
                driver
                internal
                isValid]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-info icon/networks "New network")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Create"
            :disabled   (not isValid)
            :primary    true
            :onTouchTap create-network-handler}))]]
     [:div.form-edit
      (comp/form
        {:onValid   #(state/update-value [:isValid] true cursor)
         :onInvalid #(state/update-value [:isValid] false cursor)}
        (form-name name)
        (form-driver driver)
        (form-internal internal))]]))

(defn- init-state
  []
  (state/set-value {:networkName ""
                    :driver      "bridge"
                    :internal    false
                    :isValid     false} cursor))

(defn mount!
  []
  (init-state)
  (rum/mount (form) (.getElementById js/document "content")))