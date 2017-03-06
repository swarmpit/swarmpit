(ns swarmpit.component.service.form-settings
  (:require [swarmpit.material :as material :refer [svg]]
            [swarmpit.utils :refer [remove-el]]
            [rum.core :as rum]))

(enable-console-print!)

(defonce state (atom {:image        nil
                      :serviceName  ""
                      :mode         "replicated"
                      :replicas     1
                      :autoredeploy false}))

(defn- update-item
  "Update form item configuration"
  [k v]
  (swap! state assoc k v))

(defn- form-image [value]
  (material/form-row
    "IMAGE"
    (material/select-field
      #js {:value    value
           :onChange (fn [e i v] (update-item :image v))
           :style    #js {:display  "inherit"
                          :fontSize "14px"}}
      (material/menu-item
        #js {:key         1
             :value       "image1:latest"
             :primaryText "image1:latest"})
      (material/menu-item
        #js {:key         2
             :value       "image1:latest"
             :primaryText "image2:latest"}))))

(defn- form-name [value]
  (material/form-row
    "SERVICE NAME"
    (material/text-field
      #js {:id       "serviceName"
           :value    value
           :onChange (fn [e v] (update-item :serviceName v))})))

(defn- form-mode [value]
  (material/form-row
    "MODE"
    (material/radio-button-group
      #js {:name          "mode"
           :valueSelected value
           :onChange      (fn [e v] (update-item :mode v))
           :style         #js {:display   "flex"
                               :marginTop "14px"}}
      (material/radio-button
        #js {:label "Replicated"
             :value "replicated"
             :style #js {:width "170px"}})
      (material/radio-button
        #js {:label "Global"
             :value "global"}))))

(defn- form-replicas [value]
  (material/form-row
    (str "SERVICE REPLICAS  " "(" value ")")
    (material/slider #js {:min          1
                          :max          50
                          :step         1
                          :defaultValue 1
                          :value        value
                          :onChange     (fn [e v] (update-item :replicas v))
                          :sliderStyle  #js {:marginTop "14px"}})))

(defn- form-autoredeploy [value]
  (material/form-row
    "AUTOREDEPLOY"
    (material/toogle
      #js {:toggled  value
           :onToggle (fn [e v] (update-item :autoredeploy v))
           :style    #js {:marginTop "14px"}})))

(rum/defc form < rum/reactive []
  (let [{:keys [image
                serviceName
                mode
                replicas
                autoredeploy]} (rum/react state)]
    [:div
     (form-image image)
     (form-name serviceName)
     (form-mode mode)
     (if (= "replicated" mode) (form-replicas replicas))
     (form-autoredeploy autoredeploy)]))
