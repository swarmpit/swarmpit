(ns swarmpit.component.service.form-settings
  (:require [material.component :as comp]
            [swarmpit.storage :as storage]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(enable-console-print!)

(def cursor [:page :service :wizard :settings])

(def form-mode-style
  {:display   "flex"
   :marginTop "14px"})

(def form-mode-replicated-style
  {:width "170px"})

(def form-replicas-slider-style
  {:marginTop "14px"})

(def form-image-style
  {:color "rgb(117, 117, 117)"})

(defn- form-image [value]
  (comp/form-comp
    "IMAGE"
    (comp/text-field
      {:id            "image"
       :disabled      true
       :underlineShow false
       :inputStyle    form-image-style
       :value         value})))

(defn- form-image-tag-ac [tags]
  "Preload tags for services created via swarmit"
  (comp/form-comp
    "IMAGE TAG"
    (comp/autocomplete {:id            "imageTag"
                        :onUpdateInput (fn [v] (state/update-value [:repository :imageTag] v cursor))
                        :dataSource    tags})))

(defn- form-image-tag [value]
  "For services created by docker cli there is no preload"
  (comp/form-comp
    "IMAGE TAG"
    (comp/text-field
      {:id       "imageTag"
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:repository :imageTag] v cursor))})))

(defn- form-name [value update-form?]
  (comp/form-comp
    "SERVICE NAME"
    (comp/text-field
      {:id       "serviceName"
       :disabled update-form?
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:serviceName] v cursor))})))

(defn- form-mode [value update-form?]
  (comp/form-comp
    "MODE"
    (comp/radio-button-group
      {:name          "mode"
       :style         form-mode-style
       :valueSelected value
       :onChange      (fn [_ v]
                        (state/update-value [:mode] v cursor))}
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
                       (state/update-value [:replicas] v cursor))})))

(defn image-tags-handler
  [registry repository]
  (ajax/GET (routes/path-for-backend :repository-tags {:registryName registry})
            {:headers {"Authorization" (storage/get "token")}
             :params  {:repositoryName repository}
             :handler (fn [response]
                        (state/update-value [:repository :tags] response cursor))}))

(rum/defc form < rum/reactive [update-form?]
  (let [{:keys [repository
                serviceName
                mode
                replicas]} (state/react cursor)]
    [:div.form-edit
     (form-image (:imageName repository))
     (if update-form?
       (form-image-tag (:imageTag repository))
       (form-image-tag-ac (:tags repository)))
     (form-name serviceName update-form?)
     (form-mode mode update-form?)
     (if (= "replicated" mode)
       (form-replicas replicas))]))