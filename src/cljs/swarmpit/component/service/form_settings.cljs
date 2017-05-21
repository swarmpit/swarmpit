(ns swarmpit.component.service.form-settings
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :service :settings])

(defn- form-image [value]
  (comp/form-edit-row
    "IMAGE"
    (comp/select-field
      {:value    value
       :onChange (fn [e i v]
                   (state/update-value :image v cursor))
       :style    {:display  "inherit"
                  :fontSize "14px"}}
      (comp/menu-item
        {:key         1
         :value       "nohaapav/napp:latest"
         :primaryText "nohaapav/napp:latest"})
      (comp/menu-item
        {:key         2
         :value       "nohaapav/app:latest"
         :primaryText "nohaapav/app:latest"}))))

(defn- form-name [value update-form?]
  (comp/form-edit-row
    "SERVICE NAME"
    (comp/text-field
      {:id       "serviceName"
       :disabled update-form?
       :value    value
       :onChange (fn [e v]
                   (state/update-value :serviceName v cursor))})))

(defn- form-mode [value update-form?]
  (comp/form-edit-row
    "MODE"
    (comp/radio-button-group
      {:name          "mode"
       :valueSelected value
       :onChange      (fn [e v]
                        (state/update-value :mode v cursor))
       :style         {:display   "flex"
                       :marginTop "14px"}}
      (comp/radio-button
        {:disabled update-form?
         :label    "Replicated"
         :value    "replicated"
         :style    {:width "170px"}})
      (comp/radio-button
        {:disabled update-form?
         :label    "Global"
         :value    "global"}))))

(defn- form-replicas [value]
  (comp/form-edit-row
    (str "REPLICAS  " "(" value ")")
    (comp/slider
      {:min          1
       :max          50
       :step         1
       :defaultValue 1
       :value        value
       :onChange     (fn [e v]
                       (state/update-value :replicas v cursor))
       :sliderStyle  {:marginTop "14px"}})))

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