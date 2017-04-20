(ns swarmpit.component.service.form-ports
  (:require [swarmpit.material :as material :refer [svg]]
            [swarmpit.utils :as util]
            [rum.core :as rum]))

(enable-console-print!)

(defonce state (atom []))

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
           :onChange (fn [e v] (util/update-item state index :containerPort (js/parseInt v)))})))

(defn- form-protocol [value index]
  (material/table-row-column
    #js {:key (str "protocol" index)}
    (material/select-field
      #js {:value      value
           :onChange   (fn [e i v] (util/update-item state index :protocol v))
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
           :onChange (fn [e v] (util/update-item state index :hostPort (js/parseInt v)))})))

(rum/defc form < rum/reactive []
  (let [ports (rum/react state)]
    (material/theme
      (material/table
        #js {:selectable false}
        (material/table-header-form form-headers #(util/add-item state state-item))
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
                  #(util/remove-item state index))))
            ports))))))