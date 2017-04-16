(ns swarmpit.component.network.create
  (:require [swarmpit.utils :refer [remove-el]]
            [swarmpit.material :as material]
            [swarmpit.router :as router]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [rum.core :as rum]))

(enable-console-print!)

(defonce state (atom {:name   ""
                      :driver nil}))

(defn- update-item
  "Update form item configuration"
  [k v]
  (swap! state assoc k v))

(defn- form-name [value]
  (material/form-edit-row
    "NAME"
    (material/text-field
      #js {:id       "serviceName"
           :value    value
           :onChange (fn [e v] (update-item :name v))})))

(defn- form-driver [value]
  (material/form-edit-row
    "DRIVER"
    (material/select-field
      #js {:value    value
           :onChange (fn [e i v] (update-item :driver v))
           :style    #js {:display  "inherit"
                          :fontSize "14px"}}
      (material/menu-item
        #js {:key         1
             :value       "overlay"
             :primaryText "overlay"})
      (material/menu-item
        #js {:key         2
             :value       "host"
             :primaryText "host"})
      (material/menu-item
        #js {:key         3
             :value       "bridge"
             :primaryText "bridge"}))))

(defn- create-network-handler
  []
  (POST "/networks"
        {:format        :json
         :params        state
         :finally       (progress/mount!)
         :handler       (fn [response]
                          (let [id (get response "Id")
                                message (str "Network " id " has been created.")]
                            (progress/unmount!)
                            (router/dispatch! (str "/#/networks/" id))
                            (message/mount! message)))
         :error-handler (fn [{:keys [status status-text]}]
                          (let [message (str "Network creation failed. Status: " status " Reason: " status-text)]
                            (progress/unmount!)
                            (message/mount! message)))}))

(rum/defc form < rum/reactive []
  (let [{:keys [name
                driver]} (rum/react state)]
    [:div
     [:div.form-panel
      [:div.form-panel-right
       (material/theme
         (material/raised-button
           #js {:label      "Create"
                :primary    true
                :onTouchTap create-network-handler}))]]
     [:div.form-edit
      (form-name name)
      (form-driver driver)]]))

(defn mount!
  []
  (rum/mount (form) (.getElementById js/document "content")))