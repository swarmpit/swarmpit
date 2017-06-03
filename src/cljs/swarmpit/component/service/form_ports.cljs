(ns swarmpit.component.service.form-ports
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :service :form :ports])

(def headers ["Container port" "Protocol" "Host port"])

(defn- format-port-value
  [value]
  (if (zero? value) "" value))

(defn- form-container [value index]
  (comp/table-row-column
    {:key (str "pc-" index)}
    (comp/form-list-textfield
      {:id       "containerPort"
       :type     "number"
       :value    (format-port-value value)
       :onChange (fn [_ v]
                   (state/update-item index :containerPort (js/parseInt v) cursor))})))

(defn- form-protocol [value index]
  (comp/table-row-column
    {:key (str "pp-" index)}
    (comp/form-list-selectfield
      {:value    value
       :onChange (fn [_ _ v]
                   (state/update-item index :protocol v cursor))}
      (comp/menu-item
        {:key         (str "ptcp-" index)
         :value       "tcp"
         :primaryText "TCP"})
      (comp/menu-item
        {:key         (str "pudp-" index)
         :value       "udp"
         :primaryText "UDP"}))))

(defn- form-host [value index]
  (comp/table-row-column
    {:key (str "ph-" index)}
    (comp/form-list-textfield
      {:id       "hostPort"
       :type     "number"
       :value    (format-port-value value)
       :onChange (fn [_ v]
                   (state/update-item index :hostPort (js/parseInt v) cursor))})))

(defn- render-ports
  [item index]
  (let [{:keys [containerPort
                protocol
                hostPort]} item]
    [(form-container containerPort index)
     (form-protocol protocol index)
     (form-host hostPort index)]))

(rum/defc form < rum/reactive []
  (let [ports (state/react cursor)]
    (comp/form-table headers
                     ports
                     render-ports
                     (fn [] (state/add-item {:containerPort 0
                                             :protocol      "tcp"
                                             :hostPort      0} cursor))
                     (fn [index] (state/remove-item index cursor)))))