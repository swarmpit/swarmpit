(ns swarmpit.component.service.form-ports
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :service :ports])

(def state-item {:containerPort 0
                 :protocol      "tcp"
                 :hostPort      0})

(def form-headers ["Container port" "Protocol" "Host port"])

(defn- format-port-value
  [value]
  (if (= 0 value) "" value))

(defn- form-container [value index]
  (comp/table-row-column
    {:key (str "containerPort" index)}
    (comp/text-field
      {:id       "containerPort"
       :type     "number"
       :style    {:width "100%"}
       :value    (format-port-value value)
       :onChange (fn [e v]
                   (state/update-item index :containerPort (js/parseInt v) cursor))})))

(defn- form-protocol [value index]
  (comp/table-row-column
    {:key (str "protocol" index)}
    (comp/select-field
      {:value      value
       :onChange   (fn [e i v]
                     (state/update-item index :protocol v cursor))
       :style      {:display "inherit"}
       :labelStyle {:lineHeight "45px"
                    :top        2}}
      (comp/menu-item
        {:key         (str "protocol-tcp" index)
         :value       "tcp"
         :primaryText "TCP"})
      (comp/menu-item
        {:key         (str "protocol-udp" index)
         :value       "udp"
         :primaryText "UDP"}))))

(defn- form-host [value index]
  (comp/table-row-column
    {:key (str "hostPort" index)}
    (comp/text-field
      {:id       "hostPort"
       :type     "number"
       :style    {:width "100%"}
       :value    (format-port-value value)
       :onChange (fn [e v]
                   (state/update-item index :hostPort (js/parseInt v) cursor))})))

(rum/defc form < rum/reactive []
  (let [ports (state/react cursor)]
    (comp/mui
      (comp/table
        {:selectable false}
        (comp/table-header-form form-headers #(state/add-item state-item cursor))
        (comp/table-body
          {:displayRowCheckbox false}
          (map-indexed
            (fn [index item]
              (let [{:keys [containerPort
                            protocol
                            hostPort]} item]
                (comp/table-row-form
                  index
                  [(form-container containerPort index)
                   (form-protocol protocol index)
                   (form-host hostPort index)]
                  #(state/remove-item index cursor))))
            ports))))))