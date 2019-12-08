(ns swarmpit.component.service.form-ports
  (:require [material.components :as comp]
            [material.component.form :as form]
            [material.component.list.edit :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.parser :refer [parse-int]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :ports))

(defn- not-suggested?
  [port]
  (not
    (some #(= (:containerPort port)
              (:containerPort %)) (state/get-value form-value-cursor))))

(defn load-suggestable-ports
  [repository]
  (ajax/get
    (routes/path-for-backend :repository-ports)
    {:params     {:repository    (:name repository)
                  :repositoryTag (:tag repository)}
     :on-success (fn [{:keys [response]}]
                   (doseq [port response]
                     (if (not-suggested? port)
                       (state/add-item (merge port
                                              {:mode "ingress"}) form-value-cursor))))
     :on-error   (fn [_])}))

(defn- form-container [value index]
  (comp/text-field
    {:fullWidth       true
     :key             (str "form-port-container" index)
     :label           "Container port"
     :type            "number"
     :min             1
     :max             65535
     :defaultValue    value
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-item index :containerPort (parse-int (-> % .-target .-value)) form-value-cursor)}))

(defn- form-protocol [value index]
  (comp/text-field
    {:fullWidth       true
     :key             (str "form-protocol-port-" index)
     :label           "Protocol"
     :select          true
     :value           value
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :InputProps      {:className "Swarmpit-form-input"}
     :onChange        #(state/update-item index :protocol (-> % .-target .-value) form-value-cursor)}
    (comp/menu-item
      {:key   "tcp"
       :value "tcp"} "TCP")
    (comp/menu-item
      {:key   "udp"
       :value "udp"} "UDP")))

(defn- form-mode [value index]
  (comp/text-field
    {:fullWidth       true
     :key             (str "form-port-mode-" index)
     :label           "Mode"
     :select          true
     :value           value
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :InputProps      {:className "Swarmpit-form-input"}
     :onChange        #(state/update-item index :mode (-> % .-target .-value) form-value-cursor)}
    (comp/menu-item
      {:key   "ingress"
       :value "ingress"} "ingress")
    (comp/menu-item
      {:key   "host"
       :value "host"} "host")))

(defn- form-host [value index]
  (comp/text-field
    {:fullWidth       true
     :key             (str "form-port-host-" index)
     :label           "Host port"
     :type            "number"
     :min             1
     :max             65535
     :defaultValue    value
     :variant         "outlined"
     :margin          "dense"
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-item index :hostPort (parse-int (-> % .-target .-value)) form-value-cursor)}))

(def form-ports-metadata
  [{:name      "Container port"
    :primary   true
    :key       [:containerPort]
    :render-fn (fn [value _ index] (form-container value index))}
   {:name      "Protocol"
    :key       [:protocol]
    :render-fn (fn [value _ index] (form-protocol value index))}
   {:name      "Mode"
    :key       [:mode]
    :render-fn (fn [value _ index] (form-mode value index))}
   {:name      "Host port"
    :key       [:hostPort]
    :render-fn (fn [value _ index] (form-host value index))}])

(defn form-table
  [ports]
  (list/list
    form-ports-metadata
    ports
    (fn [index] (state/remove-item index form-value-cursor))))

(defn add-item
  []
  (state/add-item {:containerPort 0
                   :protocol      "tcp"
                   :mode          "ingress"
                   :hostPort      0} form-value-cursor))

(rum/defc form < rum/reactive []
  (let [ports (state/react form-value-cursor)]
    (if (empty? ports)
      (form/item-info "Service has no published ports.")
      (form-table ports))))
