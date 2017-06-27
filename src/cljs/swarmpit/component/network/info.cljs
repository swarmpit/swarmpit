(ns swarmpit.component.network.info
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- delete-network-handler
  [network-id]
  (handler/delete
    (routes/path-for-backend :network-delete {:id network-id})
    (fn [_]
      (dispatch!
        (routes/path-for-frontend :network-list))
      (message/info
        (str "Network " network-id " has been removed.")))
    (fn [response]
      (message/error
        (str "Network removing failed. Reason: " (:error response))))))

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