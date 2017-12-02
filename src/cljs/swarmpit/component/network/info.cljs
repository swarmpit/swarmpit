(ns swarmpit.component.network.info
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [swarmpit.time :as time]
            [swarmpit.docker-utils :as utils]))

(enable-console-print!)

(defn- delete-network-handler
  [network-id]
  (handler/delete
    (routes/path-for-backend :network-delete {:id network-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :network-list))
                   (message/info
                     (str "Network " network-id " has been removed.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Network removing failed. Reason: " (:error response))))}))

(rum/defc form < rum/static [network]
  (let [stack (:stack network)
        created (:created network)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/networks
                   (:networkName network))]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:onTouchTap #(delete-network-handler (:id network))
            :label      "Delete"}))]]
     [:div.form-view
      [:div.form-view-group
       (form/section "General settings")
       (form/item "ID" (:id network))
       (if (some? stack)
         (form/item "STACK" stack))
       (form/item "NAME" (utils/trim-stack stack (:networkName network)))
       (when (time/valid? created)
         (form/item-date "CREATED" created))
       (form/item "DRIVER" (:driver network))
       (form/item "INTERNAL" (if (:internal network)
                                    "yes"
                                    "no"))]
      [:div.form-view-group
       (form/section "IP address management")
       (form/item "SUBNET" (get-in network [:ipam :subnet]))
       (form/item "GATEWAY" (get-in network [:ipam :gateway]))]]]))