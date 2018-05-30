(ns swarmpit.component.network.create
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.list-table-form :as list]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-driver-opts-cursor (conj state/form-value-cursor :options))

(def form-driver-opts-headers
  [{:name  "Name"
    :width "35%"}
   {:name  "Value"
    :width "35%"}])

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

(defn- form-attachable [value ingres?]
  (form/comp
    "IS ATTACHABLE"
    (form/checkbox
      {:checked  value
       :disabled ingres?
       :onCheck  (fn [_ v]
                   (state/update-value [:attachable] v state/form-value-cursor))})))

(defn- form-ingress [value attachable?]
  (form/comp
    "IS INGRESS"
    (form/checkbox
      {:checked  value
       :disabled attachable?
       :onCheck  (fn [_ v]
                   (state/update-value [:ingress] v state/form-value-cursor))})))

(defn- form-ipv6 [value]
  (form/comp
    "ENABLE IPV6"
    (form/checkbox
      {:checked value
       :onCheck (fn [_ v]
                  (state/update-value [:enableIPv6] v state/form-value-cursor))})))

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

(defn- form-driver-opt-name [value index]
  (list/textfield
    {:name     (str "form-driver-opt-name-" index)
     :key      (str "form-driver-opt-name-" index)
     :value    value
     :onChange (fn [_ v]
                 (state/update-item index :name v form-driver-opts-cursor))}))

(defn- form-driver-opt-value [value index]
  (list/textfield
    {:name     (str "form-driver-opt-value-" index)
     :key      (str "form-driver-opt-value-" index)
     :value    value
     :onChange (fn [_ v]
                 (state/update-item index :value v form-driver-opts-cursor))}))

(defn- form-driver-render-opts
  [item index]
  (let [{:keys [name value]} item]
    [(form-driver-opt-name name index)
     (form-driver-opt-value value index)]))

(defn- form-driver-opts-table
  [opts]
  (list/table-raw form-driver-opts-headers
                  opts
                  nil
                  form-driver-render-opts
                  (fn [index] (state/remove-item index form-driver-opts-cursor))))

(defn- add-driver-opt
  []
  (state/add-item {:name  ""
                   :value ""} form-driver-opts-cursor))

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
                    :attachable  false
                    :ingress     false
                    :enableIPv6  false
                    :options     []
                    :ipam        nil} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (init-form-value)
      (network-plugin-handler))))

(rum/defc form < rum/reactive
                 mixin-init-form [_]
  (let [{:keys [name driver internal attachable ingress enableIPv6 ipam options]} (state/react state/form-value-cursor)
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
         (form-internal internal)
         (form-attachable attachable ingress)
         (form-ingress ingress attachable)
         (form-ipv6 enableIPv6))]
      [:div.form-layout-group.form-layout-group-border
       (form/section "Driver")
       (form/form
         {}
         (form-driver driver plugins)
         (html (form/subsection-add "Add network driver option" add-driver-opt))
         (when (not (empty? options))
           (form-driver-opts-table options)))]
      [:div.form-layout-group.form-layout-group-border
       (form/section "IP address management")
       (form/form
         {:onValid   #(state/update-value [:valid-ipam?] true state/form-state-cursor)
          :onInvalid #(state/update-value [:valid-ipam?] false state/form-state-cursor)}
         (form-subnet (:subnet ipam))
         (form-gateway (:gateway ipam)))]]]))