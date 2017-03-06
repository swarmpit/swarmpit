(ns swarmpit.component.service.form-ports
  (:require [swarmpit.material :as material :refer [svg]]
            [swarmpit.utils :refer [remove-el]]
            [rum.core :as rum]))

(enable-console-print!)

(defonce state (atom []))

(def form-headers ["Container port" "Protocol" "Published" "Host port"])

(defn- add-item
  "Create new form item"
  []
  (swap! state
         (fn [p] (conj p {:containerPort ""
                          :protocol      "tcp"
                          :published     false
                          :hostPort      ""}))))

(defn- remove-item
  "Remove form item"
  [index]
  (swap! state
         (fn [p] (remove-el p index))))

(defn- update-item
  "Update form item configuration"
  [index k v]
  (swap! state
         (fn [p] (assoc-in p [index k] v))))

(defn- form-container [value index]
  (material/table-row-column
    #js {:key (str "containerPort" index)}
    (material/text-field
      #js {:id       "containerPort"
           :value    value
           :onChange (fn [e v] (update-item index :containerPort v))})))

(defn- form-protocol [value index]
  (material/table-row-column
    #js {:key (str "protocol" index)}
    (material/select-field
      #js {:value      value
           :onChange   (fn [e i v] (update-item index :protocol v))
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

(defn- form-published [value index]
  (material/table-row-column
    #js {:key (str "published" index)}
    (material/checkbox
      #js {:checked value
           :onCheck (fn [e v] (update-item index :published v))})))

(defn- form-host [value index]
  (material/table-row-column
    #js {:key (str "hostPort" index)}
    (material/text-field
      #js {:id       "hostPort"
           :value    value
           :onChange (fn [e v] (update-item index :hostPort v))})))

(rum/defc form < rum/reactive []
  (let [ports (rum/react state)]
    (material/theme
      (material/table
        #js {:selectable false}
        (material/table-header-form form-headers #(add-item))
        (material/table-body
          #js {:displayRowCheckbox false}
          (map-indexed
            (fn [index item]
              (let [{:keys [containerPort
                            protocol
                            published
                            hostPort]} item]
                (material/table-row-form
                  index
                  [(form-container containerPort index)
                   (form-protocol protocol index)
                   (form-published published index)
                   (form-host hostPort index)]
                  (fn [] (remove-item index)))))
            ports))))))