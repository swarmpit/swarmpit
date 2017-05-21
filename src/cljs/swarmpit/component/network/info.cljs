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
     (comp/form-view-section "General settings")
     (comp/form-view-row "ID" (:id item))
     (comp/form-view-row "NAME" (:name item))
     (comp/form-view-row "CREATED" (:created item))
     (comp/form-view-row "DRIVER" (:driver item))
     (comp/form-view-row "INTERNAL" (if (:internal item)
                                          "yes"
                                          "no"))
     (comp/form-view-section "IP address management")
     (comp/form-view-row "SUBNET" (:subnet item))
     (comp/form-view-row "GATEWAY" (:gateway item))]]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))