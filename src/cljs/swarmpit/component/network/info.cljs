(ns swarmpit.component.network.info
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.label :as label]
            [material.component.form :as form]
            [material.component.list.basic :as list]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.dialog :as dialog]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.list :as services]
            [swarmpit.component.common :as common]
            [swarmpit.component.toolbar :as toolbar]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.time :as time]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [clojure.string :as str]))

(enable-console-print!)

(defn- network-services-handler
  [network-id]
  (ajax/get
    (routes/path-for-backend :network-services {:id network-id})
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:services] response state/form-value-cursor))}))

(defn- network-handler
  [network-id]
  (ajax/get
    (routes/path-for-backend :network {:id network-id})
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:network] response state/form-value-cursor))}))

(defn- delete-network-handler
  [network-id]
  (ajax/delete
    (routes/path-for-backend :network {:id network-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :network-list))
                   (message/info
                     (str "Network " network-id " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Network removing failed. " (:error response))))}))

(def form-driver-opts-render-metadata
  {:primary   (fn [item] (:name item))
   :secondary (fn [item] (:value item))})

(rum/defc form-general < rum/static [{:keys [id stack networkName driver created internal attachable ingress enableIPv6 ipam]}
                                     services]
  (comp/card
    {:className "Swarmpit-form-card"}
    (when (or internal ingress attachable enableIPv6)
      (comp/card-header
        {:subheader (form/item-labels
                      [(when internal
                         (label/base "Internal" "primary"))
                       (when ingress
                         (label/base "Ingress" "primary"))
                       (when attachable
                         (label/base "Attachable" "primary"))
                       (when enableIPv6
                         (label/base "IPv6" "primary"))])}))
    (form/item-main "ID" id false)
    (form/item-main "Driver" driver)
    (form/item-main "Created" (form/item-date created))
    (when (not (str/blank? (:subnet ipam)))
      (form/item-main "Subnet" (:subnet ipam)))
    (when (not (str/blank? (:gateway ipam)))
      (form/item-main "Gateway" (:gateway ipam)))
    (when (and stack (not-empty services))
      (comp/box
        {}
        (comp/divider {})
        (comp/card-actions
          {}
          (comp/button
            {:size  "small"
             :color "primary"
             :href  (routes/path-for-frontend :stack-info {:name stack})}
            "See stack"))))))

(rum/defc form-driver < rum/static [{:keys [driver options]}]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     (comp/typography {:variant "h6"} "Driver settings")})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (list/list
        form-driver-opts-render-metadata
        options
        nil))))

(def form-actions
  [{:onClick #(state/update-value [:open] true dialog/dialog-cursor)
    :icon    (comp/svg icon/trash-path)
    :color   "default"
    :variant "outlined"
    :name    "Delete"}])

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
  (comp/mui
    (html
      [:div.Swarmpit-form
       (dialog/confirm-dialog
         #(delete-network-handler (:id network))
         "Delete network?"
         "Delete")
       (comp/container
         {:maxWidth  "md"
          :className "Swarmpit-container"}
         (comp/grid
           {:container true
            :spacing   2}
           (comp/grid
             {:item true
              :xs   12}
             (toolbar/toolbar "Network" (:networkName network) form-actions))
           (comp/grid
             {:item true
              :xs   12}
             (form-general network services))
           (when (not-empty (:options network))
             (comp/grid
               {:item true
                :xs   12}
               (form-driver network)))
           (comp/grid
             {:item true
              :xs   12}
             (services/linked services))))])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))
