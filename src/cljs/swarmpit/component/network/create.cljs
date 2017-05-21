(ns swarmpit.component.network.create
  (:require [material.component :as comp]
            [swarmpit.uri :refer [dispatch!]]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(def cursor [:form :network :create])

(defn- form-name [value]
  (comp/form-edit-row
    "NAME"
    (comp/text-field
      {:id       "serviceName"
       :value    value
       :onChange (fn [e v]
                   (state/update-value :name v cursor))})))

(defn- form-driver [value]
  (comp/form-edit-row
    "DRIVER"
    (comp/select-field
      {:value    value
       :onChange (fn [e i v]
                   (state/update-value :driver v cursor))
       :style    #js {:display  "inherit"
                      :fontSize "14px"}}
      (comp/menu-item
        {:key         1
         :value       "overlay"
         :primaryText "overlay"})
      (comp/menu-item
        {:key         2
         :value       "host"
         :primaryText "host"})
      (comp/menu-item
        {:key         3
         :value       "bridge"
         :primaryText "bridge"}))))

(defn- create-network-handler
  []
  (ajax/POST "/networks"
             {:format        :json
              :params        (state/get-value cursor)
              :finally       (progress/mount!)
              :handler       (fn [response]
                               (let [id (get response "Id")
                                     message (str "Network " id " has been created.")]
                                 (progress/unmount!)
                                 (dispatch! (str "/#/networks/" id))
                                 (message/mount! message)))
              :error-handler (fn [{:keys [status status-text]}]
                               (let [message (str "Network creation failed. Status: " status " Reason: " status-text)]
                                 (progress/unmount!)
                                 (message/mount! message)))}))

(rum/defc form < rum/reactive []
  (let [{:keys [name
                driver]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Create"
            :primary    true
            :onTouchTap create-network-handler}))]]
     [:div.form-edit
      (form-name name)
      (form-driver driver)]]))

(defn mount!
  []
  (rum/mount (form) (.getElementById js/document "content")))