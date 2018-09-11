(ns swarmpit.component.network.create
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.list.edit :as list]
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

(defn- form-name [value]
  (comp/text-field
    {:label     "Name"
     :fullWidth true
     :name      "name"
     :key       "name"
     :value     value
     :required  true
     :onChange  #(state/update-value [:networkName] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-driver [value plugins]
  (comp/form-control
    {:fullWidth true}
    (comp/input-label
      {:htmlFor "network-driver"} "Driver")
    (comp/select
      {:value    value
       :input    (comp/input
                   {:id   "network-driver"
                    :name "driver"})
       :onChange #(state/update-value [:driver] (-> % .-target .-value) state/form-value-cursor)}
      (->> plugins
           (map #(comp/menu-item
                   {:key   %
                    :value %} %))))
    (comp/form-helper-text
      {}
      "Network driver")))

(defn- form-internal [value]
  (comp/checkbox
    {:checked  value
     :value    value
     :onChange #(state/update-value [:internal] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- form-attachable [value ingres?]
  (comp/checkbox
    {:checked  value
     :disabled ingres?
     :value    value
     :onChange #(state/update-value [:attachable] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- form-ingress [value attachable?]
  (comp/checkbox
    {:checked  value
     :disabled attachable?
     :value    value
     :onChange #(state/update-value [:ingress] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- section-general
  [{:keys [name internal attachable ingress]}]
  (comp/grid
    {:container true
     :spacing   24}
    (comp/grid
      {:item true
       :xs   12}
      (form-name name))
    (comp/grid
      {:item true
       :xs   12
       :sm   6}
      (comp/form-control
        {:component "fieldset"}
        (comp/form-group
          {}
          (comp/form-control-label
            {:control (form-internal internal)
             :label   "Is Internal"})
          (comp/form-control-label
            {:control (form-attachable attachable ingress)
             :label   "Is Attachable"})
          (comp/form-control-label
            {:control (form-ingress ingress attachable)
             :label   "Is Ingress"}))))))

(defn- form-ipv6 [value]
  (comp/checkbox
    {:checked  value
     :value    value
     :onChange #(state/update-value [:enableIPv6] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- form-subnet [value]
  (comp/text-field
    {:label      "Subnet"
     :fullWidth  true
     :name       "subnet"
     :key        "subnet"
     :helperText "e.g. 10.0.0.0/24"
     :value      value
     :onChange   #(state/update-value [:ipam :subnet] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-gateway [value]
  (comp/text-field
    {:label      "Gateway"
     :fullWidth  true
     :name       "gateway"
     :key        "gateway"
     :helperText "e.g. 10.0.0.1"
     :value      value
     :onChange   #(state/update-value [:ipam :gateway] (-> % .-target .-value) state/form-value-cursor)}))

(defn- section-ipam
  [{:keys [ipam enableIPv6]}]
  (comp/grid
    {:container true
     :spacing   24}
    (comp/grid
      {:item true
       :xs   12} (form-subnet (:subnet ipam)))
    (comp/grid
      {:item true
       :xs   12} (form-gateway (:gateway ipam)))
    (comp/grid
      {:item true
       :xs   12
       :sm   6}
      (comp/form-control-label
        {:control (form-ipv6 enableIPv6)
         :label   "Enable IPV6"}))))

(def form-driver-opts-cursor (conj state/form-value-cursor :options))

(defn- form-driver-opt-name [value index]
  (comp/text-field
    {:label     "Name"
     :fullWidth true
     :name      (str "form-driver-opt-name-" index)
     :key       (str "form-driver-opt-name-" index)
     :value     value
     :onChange  #(state/update-item index :name (-> % .-target .-value) form-driver-opts-cursor)}))

(defn- form-driver-opt-value [value index]
  (comp/text-field
    {:label     "Value"
     :fullWidth true
     :name      (str "form-driver-opt-value-" index)
     :key       (str "form-driver-opt-value-" index)
     :value     value
     :onChange  #(state/update-item index :value (-> % .-target .-value) form-driver-opts-cursor)}))

(def form-driver-opts-render-metadata
  [{:name      "Name"
    :primary   true
    :key       [:name]
    :render-fn (fn [value _ index] (form-driver-opt-name value index))}
   {:name      "Value"
    :key       [:value]
    :render-fn (fn [value _ index] (form-driver-opt-value value index))}])

(defn- add-driver-opt
  []
  (state/add-item {:name  ""
                   :value ""} form-driver-opts-cursor))

(defn- section-driver
  [{:keys [driver options]} plugins]
  (comp/grid
    {:container true
     :spacing   24}
    (comp/grid
      {:item true
       :xs   12}
      (form-driver driver plugins))
    (comp/grid
      {:item true
       :xs   12
       :sm   6}
      (form/subsection
        "Driver options"
        (comp/button
          {:color   "primary"
           :onClick add-driver-opt}
          (comp/svg icon/add-small) "Add option")))
    (when (not (empty? options))
      (comp/grid
        {:item true
         :xs   12}
        (list/responsive
          form-driver-opts-render-metadata
          options
          (fn [index] (state/remove-item index form-driver-opts-cursor)))))))

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
  (let [item (state/react state/form-value-cursor)
        {:keys [valid? valid-ipam? processing? plugins]} (state/react state/form-state-cursor)]
    (comp/mui
      (html
        [:div.Swarmpit-form
         ;[:div.Swarmpit-form-panel
         ; (panel/info "New network" (comp/svg icon/networks))
         ; (comp/button
         ;   {:variant "contained"
         ;    :onClick #(create-network-handler)
         ;    :color   "primary"} "Create")]
         [:div.Swarmpit-form-context

          (comp/paper
            {:className "Swarmpit-form-context"
             :elevation 0}

            (comp/grid
              {:container true
               :spacing   40}
              (comp/grid
                {:item true
                 :xs   12
                 :sm   6}
                (comp/typography
                  {:variant      "title"
                   :gutterBottom true} "General")
                (section-general item))
              (comp/grid
                {:item true
                 :xs   12
                 :sm   6}
                (comp/typography
                  {:variant      "title"
                   :gutterBottom true} "IPAM")
                (section-ipam item))
              (comp/grid
                {:item true
                 :xs   12}
                (comp/typography
                  {:variant      "title"
                   :gutterBottom true} "Driver")
                (section-driver item plugins)))
            (html
              [:div.Swarmpit-form-buttons
               (comp/button
                 {:variant "contained"
                  :onClick #(create-network-handler)
                  :color   "primary"} "Create")]))]]))))