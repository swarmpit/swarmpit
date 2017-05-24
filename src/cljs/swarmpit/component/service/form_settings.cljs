(ns swarmpit.component.service.form-settings
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :service :settings])

(def form-image-style
  {:display  "inherit"
   :fontSize "14px"})

(def form-mode-style
  {:display   "flex"
   :marginTop "14px"})

(def form-mode-replicated-style
  {:width "170px"})

(def form-replicas-slider-style
  {:marginTop "14px"})

(defn- form-image [value]
  (comp/form-comp
    "IMAGE"
    (comp/select-field
      {:value    value
       :style    form-image-style
       :onChange (fn [_ _ v]
                   (state/update-value :image v cursor))}
      (comp/menu-item
        {:key         1
         :value       "nohaapav/napp:latest"
         :primaryText "nohaapav/napp:latest"})
      (comp/menu-item
        {:key         2
         :value       "nohaapav/app:latest"
         :primaryText "nohaapav/app:latest"}))))

(defn- form-name [value update-form?]
  (comp/form-comp
    "SERVICE NAME"
    (comp/text-field
      {:id       "serviceName"
       :disabled update-form?
       :value    value
       :onChange (fn [_ v]
                   (state/update-value :serviceName v cursor))})))

(defn- form-mode [value update-form?]
  (comp/form-comp
    "MODE"
    (comp/radio-button-group
      {:name          "mode"
       :style         form-mode-style
       :valueSelected value
       :onChange      (fn [_ v]
                        (state/update-value :mode v cursor))}
      (comp/radio-button
        {:key      "mrbr"
         :disabled update-form?
         :label    "Replicated"
         :value    "replicated"
         :style    form-mode-replicated-style})
      (comp/radio-button
        {:key      "mrbg"
         :disabled update-form?
         :label    "Global"
         :value    "global"}))))

(defn- form-replicas [value]
  (comp/form-comp
    (str "REPLICAS  " "(" value ")")
    (comp/slider
      {:min          1
       :max          50
       :step         1
       :defaultValue 1
       :value        value
       :sliderStyle  form-replicas-slider-style
       :onChange     (fn [_ v]
                       (state/update-value :replicas v cursor))})))

(rum/defc form < rum/reactive [update-form?]
  (let [{:keys [image
                serviceName
                mode
                replicas]} (state/react cursor)]
    [:div.form-edit
     (form-image image)
     (form-name serviceName update-form?)
     (form-mode mode update-form?)
     (if (= "replicated" mode)
       (form-replicas replicas))]))