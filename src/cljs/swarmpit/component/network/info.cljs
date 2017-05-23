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
     (comp/info-section "General settings")
     (comp/info-item "ID" (:id item))
     (comp/info-item "NAME" (:name item))
     (comp/info-item "CREATED" (:created item))
     (comp/info-item "DRIVER" (:driver item))
     (comp/info-item "INTERNAL" (if (:internal item)
                                          "yes"
                                          "no"))
     (comp/info-section "IP address management")
     (comp/info-item "SUBNET" (:subnet item))
     (comp/info-item "GATEWAY" (:gateway item))]]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))