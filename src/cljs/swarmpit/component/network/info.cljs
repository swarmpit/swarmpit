(ns swarmpit.component.network.info
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.label :as label]
            [material.component.form :as form]
            [material.component.list.basic :as list]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.list :as services]
            [swarmpit.docker.utils :as utils]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.time :as time]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-driver-opts-render-metadata
  {:primary   (fn [item] (:name item))
   :secondary (fn [item] (:value item))})

(defn- section-general
  [{:keys [id stack networkName driver created internal attachable ingress enableIPv6 ipam]}]
  (comp/card
    {:className "Swarmpit-form-card"}
    (comp/card-header
      {:title     networkName
       :className "Swarmpit-form-card-header"})
    (when (and (:subnet ipam)
               (:gateway ipam))
      (comp/card-content
        {}
        (html
          [:div
           [:span "The subnet is listed as " [:b (:subnet ipam)]]
           [:br]
           [:span "The gateway IP in the above instance is " [:b (:gateway ipam)]]])))
    (comp/card-content
      {}
      (form/item-labels
        [(when driver
           (label/grey driver))
         (when internal
           (label/grey "Internal"))
         (when ingress
           (label/grey "Ingress"))
         (when attachable
           (label/grey "Attachable"))
         (when enableIPv6
           (label/grey "IPv6"))]))
    (comp/card-actions
      {}
      (when stack
        (comp/button
          {:size  "small"
           :color "primary"
           :href  (routes/path-for-frontend :stack-info {:name stack})}
          "See stack")))
    (comp/divider)
    (comp/card-content
      {:style {:paddingBottom "16px"}}
      (comp/typography
        {:color "textSecondary"}
        (form/item-date created nil))
      (comp/typography
        {:color "textSecondary"}
        (form/item-id id)))))

(defn- section-driver
  [{:keys [driver options]}]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     "Driver options"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (when (not-empty options)
        (list/list
          form-driver-opts-render-metadata
          options
          nil)))))

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
    (routes/path-for-backend :network-delete {:id network-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :network-list))
                   (message/info
                     (str "Network " network-id " has been removed.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Network removing failed. " (:error response))))}))

(defn form-actions
  [{:keys [params]}]
  [{:onClick #(delete-network-handler (:id params))
    :icon    (comp/svg icon/trash)
    :name    "Delete network"}])

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
       [:div.Swarmpit-form-context
        (comp/grid
          {:container true
           :spacing   40}
          (comp/grid
            {:item true
             :xs   12
             :sm   6}
            (section-general network))
          (when (not-empty (:options network))
            (comp/grid
              {:item true
               :xs   12
               :sm   6}
              (section-driver network)))
          (when (not-empty services)
            (comp/grid
              {:item true
               :xs   12}
              (services/linked services))))]])))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    (progress/form
      (:loading? state)
      (form-info item))))