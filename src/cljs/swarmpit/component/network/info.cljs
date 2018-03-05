(ns swarmpit.component.network.info
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [material.component.list-table-auto :as list]
            [swarmpit.component.message :as message]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.list :as services]
            [swarmpit.docker.utils :as utils]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.time :as time]
            [rum.core :as rum]))

(enable-console-print!)

(defn- network-services-handler
  [network-id]
  (ajax/get
    (routes/path-for-backend :network-services {:id network-id})
    {:on-success (fn [response]
                   (state/update-value [:services] response state/form-value-cursor))}))

(defn- network-handler
  [network-id]
  (ajax/get
    (routes/path-for-backend :network {:id network-id})
    {:progress   [:loading?]
     :on-success (fn [response]
                   (state/update-value [:network] response state/form-value-cursor))}))

(defn- delete-network-handler
  [network-id]
  (ajax/delete
    (routes/path-for-backend :network-delete {:id network-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :network-list))
                   (message/info
                     (str "Network " network-id " has been removed.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Network removing failed. Reason: " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:loading? true} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (init-form-state)
      (network-handler id)
      (network-services-handler id))))

(rum/defc form-info < rum/static [{:keys [network services]}]
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
   [:div.form-layout
    [:div.form-layout-group
     (form/section "General settings")
     (form/item "ID" (:id network))
     (if (some? (:stack network))
       (form/item "STACK" (:stack network)))
     (form/item "NAME" (utils/trim-stack (:stack network)
                                         (:networkName network)))
     (when (time/valid? (:created network))
       (form/item-date "CREATED" (:created network)))
     (form/item "DRIVER" (:driver network))
     (form/item "INTERNAL" (if (:internal network)
                             "yes"
                             "no"))]
    [:div.form-layout-group.form-layout-group-border
     (form/section "IP address management")
     (form/item "SUBNET" (get-in network [:ipam :subnet]))
     (form/item "GATEWAY" (get-in network [:ipam :gateway]))]
    [:div.form-layout-group.form-layout-group-border
     (form/section "Linked Services")
     (list/table (map :name services/headers)
                 services
                 services/render-item
                 services/render-item-keys
                 services/onclick-handler)]]])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))