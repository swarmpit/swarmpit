(ns swarmpit.component.network.info
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.list.info :as list]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
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
  [{:name    "Name"
    :primary true
    :key     [:name]}
   {:name "Value"
    :key  [:value]}])

(defn- section-general
  [{:keys [id stack networkName created internal attachable ingress]}]
  (comp/card
    {:style {:height "100%"}}
    (comp/card-header
      (merge {:title     networkName
              :className "Swarmpit-form-card-header"
              ;:avatar    (comp/icon-button
              ;             {:className "Swarmpit-form-card-header-avatar"}
              ;             (comp/svg icon/networks))
              }
             (when (time/valid? created)
               {:subheader (form/item-id id)})))
    (comp/card-content
      {}
      ;(comp/typography
      ;  {:variant   "headline"
      ;   :component "h6"} "Properties")
      (comp/typography
        {:component "p"}
        (form/item-yn internal "Internal")
        (html [:br])
        (form/item-yn attachable "Attachable")
        (html [:br])
        (form/item-yn ingress "Ingress"))
      ;
      ;<Typography className={classes.pos} color="textSecondary">
      ;adjective
      ;</Typography>

      ;(form/item "ID" id)
      ;(form/item-stack stack)
      ;(form/item "NAME" (utils/trim-stack stack networkName))
      ;(form/item "INTERNAL" (if internal "yes" "no"))
      ;(form/item "ATTACHABLE" (if attachable "yes" "no"))
      ;(form/item "INGRESS" (if ingress "yes" "no"))

      )
    (comp/card-actions
      {}

      (comp/button
        {:size  "small"
         :color "primary"}
        "See stack")

      ;(comp/tooltip
      ;  {:title     "Go to stack"
      ;   :placement "top-start"}
      ;  (comp/icon-button
      ;    {:color   "secondary"
      ;     :onClick #(dispatch! (routes/path-for-frontend :stack-info {:name stack}))}
      ;    (comp/svg icon/stacks)))

      )
    (comp/divider)

    (comp/card-content
      {:style {:paddingBottom "16px"}}
      (comp/typography
        {:color "textSecondary"}
        (form/item-date "created" created)))

    ))

(defn- section-ipam
  [{:keys [ipam enableIPv6]}]
  (comp/card
    {:style {:height "100%"}}
    (comp/card-header
      {:title     "IP address management"
       :className "Swarmpit-form-card-header"})
    (comp/card-content
      {}
      (form/item "SUBNET" (:subnet ipam))
      (form/item "GATEWAY" (:gateway ipam))
      (form/item "ENABLED IPv6" (if enableIPv6 "yes" "no")))))

(defn- section-driver
  [{:keys [driver options]}]
  (comp/card
    {:style {:height "100%"}}
    (comp/card-header
      {:title     "Driver"
       :className "Swarmpit-form-card-header"})
    (comp/card-content
      {}
      (form/item "NAME" driver)
      (html
        [:div.Swarmpit-form-section-wrapper
         (form/subsection "Driver options")])
      (when (not-empty options)
        (list/responsive
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

(defn delete-button []
  (comp/icon-button
    {:onClick #(delete-network-handler "tests")
     :color   "inherit"} (comp/svg icon/trash)))

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
  (let [subnet (get-in network [:ipam :subnet])
        gateway (get-in network [:ipam :gateway])]
    (comp/mui
      (html
        [:div.Swarmpit-form
         ;[:div.Swarmpit-form-panel
         ; (panel/info (:networkName network) (comp/svg icon/networks))
         ; (comp/button
         ;   {:variant "contained"
         ;    :onClick #(delete-network-handler (:id network))
         ;    :color   "primary"} "Delete")]
         [:div.Swarmpit-form-context
          (comp/grid
            {:container true
             :spacing   40}
            (comp/grid
              {:item true
               :xs   12
               :sm   6}
              (section-general network))
            (when (and (some? subnet)
                       (some? gateway))
              (comp/grid
                {:item true
                 :xs   12
                 :sm   6}
                (section-ipam network)))
            (comp/grid
              {:item true
               :xs   12
               :sm   6}
              (section-driver network)))]]))






    ;[:div
    ; [:div.form-panel
    ;  [:div.form-panel-left
    ;   (panel/info icon/networks
    ;               (:networkName network))]
    ;  [:div.form-panel-right
    ;   (comp/mui
    ;     (comp/raised-button
    ;       {:onTouchTap #(delete-network-handler (:id network))
    ;        :label      "Delete"}))]]
    ; [:div.form-layout
    ;  [:div.form-layout-group
    ;   (form/subsection "General settings")
    ;   (form/item "ID" (:id network))
    ;   (form/item-stack (:stack network))
    ;   (form/item "NAME" (utils/trim-stack (:stack network)
    ;                                       (:networkName network)))
    ;   (when (time/valid? (:created network))
    ;     (form/item-date "CREATED" (:created network)))
    ;   (form/item "INTERNAL" (if (:internal network) "yes" "no"))
    ;   (form/item "ATTACHABLE" (if (:attachable network) "yes" "no"))
    ;   (form/item "INGRESS" (if (:ingress network) "yes" "no"))
    ;   (form/item "ENABLED IPv6" (if (:enableIPv6 network) "yes" "no"))]
    ;  [:div.form-layout-group.form-layout-group-border
    ;   (form/subsection "Driver")
    ;   (form/item "NAME" (:driver network))
    ;   (when (not-empty (:options network))
    ;     [:div
    ;      (form/subsection "Network driver options")
    ;      (list/table driver-opts-headers
    ;                  (:options network)
    ;                  driver-opts-render-item
    ;                  driver-opts-render-keys
    ;                  nil)])]
    ;  (when (and (some? subnet)
    ;             (some? gateway))
    ;    [:div.form-layout-group.form-layout-group-border
    ;     (form/subsection "IP address management")
    ;     (form/item "SUBNET" subnet)
    ;     (form/item "GATEWAY" gateway)])
    ;  (services/linked-services services)]]

    ))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [state (state/react state/form-state-cursor)
        item (state/react state/form-value-cursor)]
    ;(progress/form
    ;  (:loading? state)
    ;  (form-info item))

    (when (false? (:loading? state))
      (form-info item))

    ;(form-info item)

    ))