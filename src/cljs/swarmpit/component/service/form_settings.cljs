(ns swarmpit.component.service.form-settings
  (:require [swarmpit.component.state :as state]
            [swarmpit.material :as material]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :service :settings])

(defn- form-image [value]
  (material/form-edit-row
    "IMAGE"
    (material/select-field
      #js {:value    value
           :onChange (fn [e i v]
                       (state/update-value :image v cursor))
           :style    #js {:display  "inherit"
                          :fontSize "14px"}}
      (material/menu-item
        #js {:key         1
             :value       "nohaapav/napp:latest"
             :primaryText "nohaapav/napp:latest"})
      (material/menu-item
        #js {:key         2
             :value       "nohaapav/app:latest"
             :primaryText "nohaapav/app:latest"}))))

(defn- form-name [value update-form?]
  (material/form-edit-row
    "SERVICE NAME"
    (material/text-field
      #js {:id       "serviceName"
           :disabled update-form?
           :value    value
           :onChange (fn [e v]
                       (state/update-value :serviceName v cursor))})))

(defn- form-mode [value update-form?]
  (material/form-edit-row
    "MODE"
    (material/radio-button-group
      #js {:name          "mode"
           :valueSelected value
           :onChange      (fn [e v]
                            (state/update-value :mode v cursor))
           :style         #js {:display   "flex"
                               :marginTop "14px"}}
      (material/radio-button
        #js {:disabled update-form?
             :label    "Replicated"
             :value    "replicated"
             :style    #js {:width "170px"}})
      (material/radio-button
        #js {:disabled update-form?
             :label    "Global"
             :value    "global"}))))

(defn- form-replicas [value]
  (material/form-edit-row
    (str "REPLICAS  " "(" value ")")
    (material/slider #js {:min          1
                          :max          50
                          :step         1
                          :defaultValue 1
                          :value        value
                          :onChange     (fn [e v]
                                          (state/update-value :replicas v cursor))
                          :sliderStyle  #js {:marginTop "14px"}})))

(rum/defc form < rum/reactive [update-form?]
  (let [{:keys [image
                serviceName
                mode
                replicas]} (state/react cursor)]
    [:div.form-edit
     (form-image image)
     (form-name serviceName update-form?)
     (form-mode mode update-form?)
     (if (= "replicated" mode) (form-replicas replicas))]))