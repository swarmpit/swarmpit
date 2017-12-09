(ns swarmpit.component.network.info
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.routes :as routes]
            [swarmpit.time :as time]
            [swarmpit.docker-utils :as utils]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(defn- network-handler
  [network-id]
  (handler/get
    (routes/path-for-backend :network {:id network-id})
    {:on-success (fn [response]
                   (state/set-value response cursor))}))

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

(def mixin-init-state
  (mixin/init-state
    (fn [{:keys [id]}]
      (network-handler id))))

(rum/defc form-info < rum/static [network]
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

(rum/defc form < rum/reactive
                 mixin-init-state [_]
  (let [network (state/react cursor)]
    (progress/form
      (nil? network)
      (form-info network))))