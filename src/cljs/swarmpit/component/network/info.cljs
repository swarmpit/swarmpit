(ns swarmpit.component.network.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(defn- delete-network-info-msg
  [id]
  (str "Network " id " has been removed."))

(defn- delete-network-error-msg
  [error]
  (str "Network removing failed. Reason: " error))

(defn- delete-network-handler
  [network-id]
  (ajax/DELETE (routes/path-for-backend :network-delete {:id network-id})
               {:headers       {"Authorization" (storage/get "token")}
                :handler       (fn [_]
                                 (dispatch!
                                   (routes/path-for-frontend :network-list))
                                 (message/mount!
                                   (delete-network-info-msg network-id)))
                :error-handler (fn [{:keys [response]}]
                                 (let [error (get response "error")]
                                   (message/mount!
                                     (delete-network-error-msg error) true)))}))

(rum/defc form < rum/static [item]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (comp/panel-info icon/networks
                      (:networkName item))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-network-handler (:id item))
          :label      "Delete"}))]]
   [:div.form-view
    [:div.form-view-group
     (comp/form-section "General settings")
     (comp/form-item "ID" (:id item))
     (comp/form-item "NAME" (:networkName item))
     (comp/form-item "CREATED" (:created item))
     (comp/form-item "DRIVER" (:driver item))
     (comp/form-item "INTERNAL" (if (:internal item)
                                  "yes"
                                  "no"))]
    [:div.form-view-group
     (comp/form-section "IP address management")
     (comp/form-item "SUBNET" (get-in item [:ipam :subnet]))
     (comp/form-item "GATEWAY" (get-in item [:ipam :gateway]))]]])

(defn mount!
  [item]
  (rum/mount (form item) (.getElementById js/document "content")))