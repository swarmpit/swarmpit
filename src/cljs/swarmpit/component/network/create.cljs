(ns swarmpit.component.network.create
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.composite :as composite]
            [material.component.list.edit :as list]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def doc-network-link "https://docs.docker.com/network/")

(defn- form-name [value]
  (comp/text-field
    {:label           "Name"
     :fullWidth       true
     :name            "name"
     :key             "name"
     :margin          "normal"
     :variant         "outlined"
     :style           {:maxWidth "350px"}
     :defaultValue    value
     :required        true
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:networkName] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-driver [value plugins]
  (comp/text-field
    {:fullWidth       true
     :key             "driver"
     :label           "Network driver"
     :helperText      "Driver to manage the Network "
     :style           {:maxWidth "350px"}
     :select          true
     :value           value
     :margin          "normal"
     :variant         "outlined"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:driver] (-> % .-target .-value) state/form-value-cursor)}
    (->> plugins
         (map #(comp/menu-item
                 {:key   %
                  :value %} %)))))

(defn- form-internal [value]
  (comp/checkbox
    {:checked  value
     :key      "internal"
     :value    (str value)
     :onChange #(state/update-value [:internal] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- form-attachable [value ingres?]
  (comp/checkbox
    {:checked  value
     :key      "attachable"
     :disabled ingres?
     :value    (str value)
     :onChange #(state/update-value [:attachable] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- form-ingress [value attachable?]
  (comp/checkbox
    {:checked  value
     :key      "ingress"
     :disabled attachable?
     :value    (str value)
     :onChange #(state/update-value [:ingress] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- section-general
  [{:keys [networkName internal attachable ingress]}]
  (comp/grid
    {:container true
     :key       "sgg"
     :spacing   24}
    (comp/grid
      {:item true
       :key  "sggn"
       :xs   12}
      (form-name networkName))
    (comp/grid
      {:item true
       :key  "sgfcg"
       :xs   12
       :sm   6}
      (comp/form-control
        {:component "fieldset"
         :key       "sgfc"}
        (comp/form-group
          {:key "sgfcg"}
          (comp/form-control-label
            {:control (form-internal internal)
             :key     "sgfclint"
             :label   "Is Internal"})
          (comp/form-control-label
            {:control (form-attachable attachable ingress)
             :key     "sgfclat"
             :label   "Is Attachable"})
          (comp/form-control-label
            {:control (form-ingress ingress attachable)
             :key     "sgfcling"
             :label   "Is Ingress"}))))))

(defn- form-ipv6 [value]
  (comp/checkbox
    {:checked  value
     :key      "ipv6"
     :value    (str value)
     :onChange #(state/update-value [:enableIPv6] (-> % .-target .-checked) state/form-value-cursor)}))

(defn- form-subnet [value]
  (comp/text-field
    {:label           "Subnet"
     :fullWidth       true
     :variant         "outlined"
     :name            "subnet"
     :key             "subnet"
     :placeholder     "e.g. 10.0.0.0/24"
     :helperText      "Subnet in CIDR format that represents a network segment"
     :style           {:maxWidth "350px"}
     :margin          "normal"
     :defaultValue    value
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:ipam :subnet] (-> % .-target .-value) state/form-value-cursor)}))

(defn- form-gateway [value]
  (comp/text-field
    {:label           "Gateway"
     :fullWidth       true
     :variant         "outlined"
     :name            "gateway"
     :key             "gateway"
     :placeholder     "e.g. 10.0.0.1"
     :helperText      "IPv4 or IPv6 Gateway for the master subnet"
     :style           {:maxWidth "350px"}
     :defaultValue    value
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:ipam :gateway] (-> % .-target .-value) state/form-value-cursor)}))

(defn- section-ipam
  [{:keys [ipam enableIPv6]}]
  (comp/grid
    {:container true
     :key       "nsicg"
     :spacing   24}
    (comp/grid
      {:item true
       :key  "nsiigs"
       :xs   12}
      (form-subnet (:subnet ipam)))
    (comp/grid
      {:item true
       :key  "nsiigg"
       :xs   12}
      (form-gateway (:gateway ipam)))
    (comp/grid
      {:item true
       :key  "nsiigipv6"
       :xs   12
       :sm   6}
      (comp/form-control-label
        {:control (form-ipv6 enableIPv6)
         :key     "nsiigipv6cl"
         :label   "Enable IPV6"}))))

(def form-driver-opts-cursor (conj state/form-value-cursor :options))

(defn- form-driver-opt-name [value index]
  (comp/text-field
    {:placeholder  "Name"
     :variant      "outlined"
     :margin       "dense"
     :fullWidth    true
     :name         (str "form-driver-opt-name-" index)
     :key          (str "form-driver-opt-name-" index)
     :defaultValue value
     :onChange     #(state/update-item index :name (-> % .-target .-value) form-driver-opts-cursor)}))

(defn- form-driver-opt-value [value index]
  (comp/text-field
    {:placeholder  "Value"
     :variant      "outlined"
     :margin       "dense"
     :fullWidth    true
     :name         (str "form-driver-opt-value-" index)
     :key          (str "form-driver-opt-value-" index)
     :defaultValue value
     :onChange     #(state/update-item index :value (-> % .-target .-value) form-driver-opts-cursor)}))

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
     :key       "nsdcg"
     :spacing   24}
    (comp/grid
      {:item true
       :key  "nsdigd"
       :xs   12}
      (form-driver driver plugins))
    (comp/grid
      {:item true
       :key  "nsdigo"
       :xs   12
       :sm   6}
      (form/subsection
        "Driver options"
        (comp/button
          {:color   "primary"
           :key     "nsdigob"
           :onClick add-driver-opt}
          (comp/svg
            {:key "nsdigobi"} icon/add-small-path) "Add option")))
    (when (not (empty? options))
      (comp/grid
        {:item true
         :key  "nsdigol"
         :xs   12}
        (rum/with-key
          (list/list
            form-driver-opts-render-metadata
            options
            (fn [index] (state/remove-item index form-driver-opts-cursor))) "nsdigoll")))))

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
  (state/set-value {:networkName ""
                    :driver      "overlay"
                    :internal    false
                    :attachable  false
                    :ingress     false
                    :enableIPv6  false
                    :options     []
                    :ipam        {:subnet  ""
                                  :gateway ""}} state/form-value-cursor))

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
         [:div.Swarmpit-form-context
          (comp/grid
            {:container true
             :key       "sccg"
             :spacing   40}
            (comp/grid
              {:item true
               :key  "snoccgif"
               :xs   12
               :sm   12
               :md   12
               :lg   8
               :xl   8}
              (comp/card
                {:className "Swarmpit-form-card"
                 :key       "ncc"}
                (comp/card-header
                  {:className "Swarmpit-form-card-header"
                   :title     "New Network"
                   :key       "ncch"})
                (comp/card-content
                  {:key "nccc"}
                  (comp/grid
                    {:container true
                     :key       "nccccg"
                     :spacing   40}
                    (comp/grid
                      {:item true
                       :key  "ncccigg"
                       :xs   12
                       :sm   6}
                      (comp/typography
                        {:variant      "h6"
                         :key          "nccciggt"
                         :gutterBottom true} "General")
                      (section-general item))
                    (comp/grid
                      {:item true
                       :key  "nccciig"
                       :xs   12
                       :sm   6}
                      (comp/typography
                        {:variant      "h6"
                         :key          "nccciigt"
                         :gutterBottom true} "IPAM")
                      (section-ipam item))
                    (comp/grid
                      {:item true
                       :key  "ncccidg"
                       :xs   12}
                      (comp/typography
                        {:variant      "h6"
                         :key          "ncccidgt"
                         :gutterBottom true} "Driver")
                      (section-driver item plugins)))
                  (html
                    [:div {:class "Swarmpit-form-buttons"
                           :key   "ncccbtn"}
                     (composite/progress-button
                       "Create"
                       #(create-network-handler)
                       processing?)]))))
            (comp/grid
              {:item true
               :key  "snoccgid"
               :xs   12
               :sm   12
               :md   12
               :lg   4
               :xl   4}
              (form/open-in-new "Learn more about networks" doc-network-link)))]]))))