(ns swarmpit.component.service.form-settings
  (:require [material.component :as comp]
            [material.component.form :as form]
            [swarmpit.component.state :as state]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :settings))

(def form-state-cursor (conj state/form-state-cursor :settings))

(def form-mode-style
  {:display   "flex"
   :marginTop "14px"})

(def form-image-style
  {:color "rgb(117, 117, 117)"})

(defn- form-image [value]
  (form/comp
    "IMAGE"
    (comp/vtext-field
      {:name          "image"
       :key           "image"
       :required      true
       :disabled      true
       :underlineShow false
       :inputStyle    form-image-style
       :value         value})))

(defn- form-image-tag [value tags]
  "For update services there is no port preload"
  (form/comp
    "IMAGE TAG"
    (comp/autocomplete
      {:name          "image-tag"
       :key           "image-tag"
       :searchText    (:tag value)
       :onUpdateInput (fn [v] (state/update-value [:repository :tag] v form-value-cursor))
       :dataSource    tags})))

(defn- form-image-tag-preloaded [value tags]
  "Preload ports for services created via swarmpit"
  (form/comp
    "IMAGE TAG"
    (comp/autocomplete {:name          "imageTagAuto"
                        :key           "imageTagAuto"
                        :searchText    (:tag value)
                        :onUpdateInput (fn [v] (state/update-value [:repository :tag] v form-value-cursor))
                        :onNewRequest  (fn [_] (ports/load-suggestable-ports value))
                        :dataSource    tags})))

(defn- form-name [value update-form?]
  (form/comp
    "SERVICE NAME"
    (comp/vtext-field
      {:name     "service-name"
       :key      "service-name"
       :required true
       :disabled update-form?
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:serviceName] v form-value-cursor))})))

(defn- form-mode [value update-form?]
  (form/comp
    "MODE"
    (comp/radio-button-group
      {:name          "mode"
       :key           "mode"
       :style         form-mode-style
       :valueSelected value
       :onChange      (fn [_ v]
                        (state/update-value [:mode] v form-value-cursor))}
      (comp/radio-button
        {:name     "replicated-mode"
         :key      "replicated-mode"
         :disabled update-form?
         :label    "Replicated"
         :value    "replicated"})
      (comp/radio-button
        {:name     "global-mode"
         :key      "global-mode"
         :disabled update-form?
         :label    "Global"
         :value    "global"}))))

(defn- form-replicas [value]
  (form/comp
    "REPLICAS"
    (comp/vtext-field
      {:name     "replicas"
       :key      "replicas"
       :required true
       :type     "number"
       :min      0
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:replicas] (js/parseInt v) form-value-cursor))})))

(defn tags-handler
  [repository]
  (ajax/get
    (routes/path-for-backend :repository-tags)
    {:params     {:repository repository}
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:tags] response form-state-cursor))
     :on-error   (fn [_])}))

(rum/defc form < rum/reactive [update-form?]
  (let [{:keys [repository serviceName mode replicas]} (state/react form-value-cursor)
        {:keys [tags]} (state/react form-state-cursor)]
    [:div.form-edit
     (form/form
       {:onValid   #(state/update-value [:valid?] true form-state-cursor)
        :onInvalid #(state/update-value [:valid?] false form-state-cursor)}
       (form-image (:name repository))
       (if update-form?
         (form-image-tag repository tags)
         (form-image-tag-preloaded repository tags))
       (form-name serviceName update-form?)
       (form-mode mode update-form?)
       (when (= "replicated" mode)
         (form-replicas replicas)))]))