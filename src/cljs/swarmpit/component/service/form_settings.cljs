(ns swarmpit.component.service.form-settings
  (:require [material.component :as comp]
            [material.component.form :as form]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :settings])

(defonce valid? (atom false))

(defonce tags (atom []))

(defn tags-handler
  [repository]
  (handler/get 
    (routes/path-for-backend :repository-tags)
    {:params     {:repository repository}
     :on-success (fn [response]
                   (reset! tags response))
     :on-error   (fn [_])}))

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
       :onUpdateInput (fn [v] (state/update-value [:repository :tag] v cursor))
       :dataSource    tags})))

(defn- form-image-tag-preloaded [value tags]
  "Preload ports for services created via swarmpit"
  (form/comp
    "IMAGE TAG"
    (comp/autocomplete {:name          "imageTagAuto"
                        :key           "imageTagAuto"
                        :searchText    (:tag value)
                        :onUpdateInput (fn [v] (state/update-value [:repository :tag] v cursor))
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
                   (state/update-value [:serviceName] v cursor))})))

(defn- form-mode [value update-form?]
  (form/comp
    "MODE"
    (comp/radio-button-group
      {:name          "mode"
       :key           "mode"
       :style         form-mode-style
       :valueSelected value
       :onChange      (fn [_ v]
                        (state/update-value [:mode] v cursor))}
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
                   (state/update-value [:replicas] (js/parseInt v) cursor))})))

(rum/defc form < rum/reactive [update-form?]
  (let [{:keys [repository
                serviceName
                mode
                replicas]} (state/react cursor)]
    [:div.form-edit
     (form/form
       {:onValid   #(reset! valid? true)
        :onInvalid #(reset! valid? false)}
       (form-image (:name repository))
       (if update-form?
         (form-image-tag repository (rum/react tags))
         (form-image-tag-preloaded repository (rum/react tags)))
       (form-name serviceName update-form?)
       (form-mode mode update-form?)
       (when (= "replicated" mode)
         (form-replicas replicas)))]))