(ns swarmpit.component.network.create
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn- form-name [value]
  (form/comp
    "NAME"
    (comp/vtext-field
      {:name     "name"
       :key      "name"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:networkName] v state/form-value-cursor))})))

(defn- form-driver [value plugins]
  (form/comp
    "DRIVER"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:driver] v state/form-value-cursor))}
      (->> plugins
           (map #(comp/menu-item
                   {:key         %
                    :value       %
                    :primaryText %}))))))

(defn- form-internal [value]
  (form/comp
    "IS PRIVATE"
    (form/checkbox
      {:checked value
       :onCheck (fn [_ v]
                  (state/update-value [:internal] v state/form-value-cursor))})))

(defn- form-subnet [value]
  (form/comp
    "SUBNET"
    (comp/vtext-field
      {:name            "subnet"
       :key             "subnet"
       :validations     "isValidSubnet"
       :validationError "Please provide a valid CIDR format"
       :hintText        "e.g. 10.0.0.0/24"
       :value           value
       :onChange        (fn [_ v]
                          (state/update-value [:ipam :subnet] v state/form-value-cursor))})))

(defn- form-gateway [value]
  (form/comp
    "GATEWAY"
    (comp/vtext-field
      {:name            "gateway"
       :key             "gateway"
       :validations     "isValidGateway"
       :validationError "Please provide a valid IP format"
       :hintText        "e.g. 10.0.0.1"
       :value           value
       :onChange        (fn [_ v]
                          (state/update-value [:ipam :gateway] v state/form-value-cursor))})))

(defn- network-plugin-handler
  []
  (ajax/get
    (routes/path-for-backend :plugin-network)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:plugins] response state/form-state-cursor))}))

(defn- create-network-handler
  []
  (ajax/post
    (routes/path-for-backend :network-create)
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :network-info (select-keys response [:id]))))
                   (message/info
                     (str "Network " (:Id response) " has been added.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Network creation failed. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:valid?      false
                    :valid-ipam? true
                    :processing? false
                    :plugins     []} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:networkName nil
                    :driver      "overlay"
                    :internal    false
                    :ipam        nil} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (init-form-value)
      (network-plugin-handler))))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [{:keys [name driver internal ipam]} (state/react state/form-value-cursor)
        {:keys [valid? valid-ipam? processing? plugins]} (state/react state/form-state-cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/networks "New network")]
      [:div.form-panel-right
       (comp/progress-button
         {:label      "Create"
          :disabled   (or (not valid?)
                          (not valid-ipam?))
          :primary    true
          :onTouchTap create-network-handler} processing?)]]
     [:div.form-layout
      [:div.form-layout-group
       (form/section "General settings")
       (form/form
         {:onValid   #(state/update-value [:valid?] true state/form-state-cursor)
          :onInvalid #(state/update-value [:valid?] false state/form-state-cursor)}
         (form-name name)
         (form-driver driver plugins)
         (form-internal internal))]
      [:div.form-layout-group.form-layout-group-border
       (form/section "IP address management")
       (form/form
         {:onValid   #(state/update-value [:valid-ipam?] true state/form-state-cursor)
          :onInvalid #(state/update-value [:valid-ipam?] false state/form-state-cursor)}
         (form-subnet (:subnet ipam))
         (form-gateway (:gateway ipam)))]

      ]]))