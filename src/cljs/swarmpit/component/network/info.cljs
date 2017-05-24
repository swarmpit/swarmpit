(ns swarmpit.component.network.info
  (:require [material.component :as comp]
            [swarmpit.uri :refer [dispatch!]]
            [swarmpit.component.message :as message]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(defn- delete-network-handler
  [network-id]
  (ajax/DELETE (str "/networks/" network-id)
               {:handler       (fn [_]
                                 (let [message (str "Network " network-id " has been removed.")]
                                   (dispatch! "/#/networks")
                                   (message/mount! message)))
                :error-handler (fn [{:keys [status status-text]}]
                                 (let [message (str "Network " network-id " removing failed. Reason: " status-text)]
                                   (message/mount! message)))}))

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:onTouchTap delete-network-handler
          :label      "Delete"}))]]
   [:div.form-view
    [:div.form-view-group
     (comp/form-section "General settings")
     (comp/form-item "ID" (:id item))
     (comp/form-item "NAME" (:name item))
     (comp/form-item "CREATED" (:created item))
     (comp/form-item "DRIVER" (:driver item))
     (comp/form-item "INTERNAL" (if (:internal item)
                                          "yes"
                                          "no"))
     (comp/form-section "IP address management")
     (comp/form-item "SUBNET" (:subnet item))
     (comp/form-item "GATEWAY" (:gateway item))]]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))