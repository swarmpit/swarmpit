(ns swarmpit.component.service.form-ports
  (:require [swarmpit.component.state :as state]
            [swarmpit.material :as material]
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
  (material/table-row-column
    #js {:key (str "containerPort" index)}
    (material/text-field
      #js {:id       "containerPort"
           :type     "number"
           :style    #js {:width "100%"}
           :value    (format-port-value value)
           :onChange (fn [e v]
                       (state/update-item index :containerPort (js/parseInt v) cursor))})))

(defn- form-protocol [value index]
  (material/table-row-column
    #js {:key (str "protocol" index)}
    (material/select-field
      #js {:value      value
           :onChange   (fn [e i v]
                         (state/update-item index :protocol v cursor))
           :style      #js {:display "inherit"}
           :labelStyle #js {:lineHeight "45px"
                            :top        2}}
      (material/menu-item
        #js {:key         (str "protocol-tcp" index)
             :value       "tcp"
             :primaryText "TCP"})
      (material/menu-item
        #js {:key         (str "protocol-udp" index)
             :value       "udp"
             :primaryText "UDP"}))))

(defn- form-host [value index]
  (material/table-row-column
    #js {:key (str "hostPort" index)}
    (material/text-field
      #js {:id       "hostPort"
           :type     "number"
           :style    #js {:width "100%"}
           :value    (format-port-value value)
           :onChange (fn [e v]
                       (state/update-item index :hostPort (js/parseInt v) cursor))})))

(rum/defc form < rum/reactive []
  (let [ports (state/react cursor)]
    (material/theme
      (material/table
        #js {:selectable false}
        (material/table-header-form form-headers #(state/add-item state-item cursor))
        (material/table-body
          #js {:displayRowCheckbox false}
          (map-indexed
            (fn [index item]
              (let [{:keys [containerPort
                            protocol
                            hostPort]} item]
                (material/table-row-form
                  index
                  [(form-container containerPort index)
                   (form-protocol protocol index)
                   (form-host hostPort index)]
                  #(state/remove-item index cursor))))
            ports))))))