(ns swarmpit.component.volume.create
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

(def cursor [:page :volume :form])

(defn- form-name [value]
  (comp/form-comp
    "NAME"
    (comp/vtext-field
      {:name     "name"
       :key      "name"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:volumeName] v cursor))})))

(defn- form-driver [value]
  (comp/form-comp
    "DRIVER"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:driver] v cursor))}
      (comp/menu-item
        {:key         "fdi1"
         :value       "local"
         :primaryText "local"}))))

(defn- create-volume-handler
  []
  (ajax/POST (routes/path-for-backend :volume-create)
             {:format        :json
              :headers       {"Authorization" (storage/get "token")}
              :params        (state/get-value cursor)
              :finally       (progress/mount!)
              :handler       (fn [response]
                               (let [id (get response "Id")
                                     message (str "Volume " id " has been created.")]
                                 (progress/unmount!)
                                 (dispatch!
                                   (routes/path-for-frontend :volume-info {:id id}))
                                 (message/mount! message)))
              :error-handler (fn [{:keys [response]}]
                               (let [error (get response "error")
                                     message (str "Volume creation failed. Reason: " error)]
                                 (progress/unmount!)
                                 (message/mount! message)))}))

(rum/defc form < rum/reactive []
  (let [{:keys [name
                driver
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
            :onTouchTap create-volume-handler}))]]

     [:div.form-view
      [:div.form-view-group
       (comp/form-section "General settings")
       (comp/form
         {:onValid   #(state/update-value [:isValid] true cursor)
          :onInvalid #(state/update-value [:isValid] false cursor)}
         (form-name name)
         (form-driver driver))]]]))

(defn- init-state
  []
  (state/set-value {:networkName nil
                    :driver      "local"
                    :isValid     false} cursor))

(defn mount!
  []
  (init-state)
  (rum/mount (form) (.getElementById js/document "content")))