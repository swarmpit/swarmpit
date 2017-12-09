(ns swarmpit.component.network.create
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(defonce isValid (atom false))

(defonce isValidIpam (atom true))

(defonce network-plugins (atom []))

(defn- network-plugin-handler
  []
  (handler/get
    (routes/path-for-backend :plugin-network)
    {:on-success (fn [response]
                   (reset! network-plugins response))}))

(defn- form-name [value]
  (form/comp
    "NAME"
    (comp/vtext-field
      {:name     "name"
       :key      "name"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:networkName] v cursor))})))

(defn- form-driver [value plugins]
  (form/comp
    "DRIVER"
    (comp/select-field
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-value [:driver] v cursor))}
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
                  (state/update-value [:internal] v cursor))})))

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
                          (state/update-value [:ipam :subnet] v cursor))})))

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
                          (state/update-value [:ipam :gateway] v cursor))})))

(defn- create-network-handler
  []
  (handler/post
    (routes/path-for-backend :network-create)
    {:params     (state/get-value cursor)
     :on-success (fn [response]
                   (dispatch!
                     (routes/path-for-frontend :network-info {:id (:id response)}))
                   (message/info
                     (str "Network " (:Id response) " has been added.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Network creation failed. Reason: " (:error response))))}))

(defn- init-state
  []
  (state/set-value {:networkName nil
                    :driver      "overlay"
                    :internal    false
                    :ipam        nil} cursor))

(def mixin-init-state
  (mixin/init-state
    (fn []
      (init-state)
      (network-plugin-handler))))

(rum/defc form < rum/reactive
                 mixin-init-state []
  (let [{:keys [name
                driver
                internal
                ipam]} (state/react cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/networks "New network")]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:label      "Create"
            :disabled   (or (not (rum/react isValid))
                            (not (rum/react isValidIpam)))
            :primary    true
            :onTouchTap create-network-handler}))]]
     [:div.form-view
      [:div.form-view-group
       (form/section "General settings")
       (form/form
         {:onValid   #(reset! isValid true)
          :onInvalid #(reset! isValid false)}
         (form-name name)
         (form-driver driver (rum/react network-plugins))
         (form-internal internal))]
      [:div.form-view-group
       (form/section "IP address management")
       (form/form
         {:onValid   #(reset! isValidIpam true)
          :onInvalid #(reset! isValidIpam false)}
         (form-subnet (:subnet ipam))
         (form-gateway (:gateway ipam)))]]]))