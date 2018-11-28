(ns swarmpit.component.service.form-settings
  (:require [goog.object]
            [material.component :as comp]
            [swarmpit.component.state :as state]
            [material.component.composite :as composite]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-networks :as networks]
            [swarmpit.component.parser :refer [parse-int]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [clojure.string :as str]
            [sablono.core :refer-macros [html]]
            [clojure.walk :refer [keywordize-keys]]
            [rum.core :as rum]))

(enable-console-print!)

(def form-value-cursor (conj state/form-value-cursor :settings))

(def form-state-cursor (conj state/form-state-cursor :settings))

(defn- form-image [value]
  (comp/text-field
    {:key             "image"
     :label           "Image"
     :variant         "outlined"
     :margin          "normal"
     :fullWidth       true
     :disabled        true
     :required        true
     :value           value
     :InputLabelProps {:shrink true}}))

(defn- form-image-tag [value tags]
  "For update services there is no port preload"
  (let [suggestions (map #(hash-map :label %
                                    :value %) tags)]
    (composite/autocomplete
      {:options        suggestions
       :textFieldProps {:label           "Tag"
                        :margin          "normal"
                        :helperText      "Specify image tag or leave empty for latest"
                        :InputLabelProps {:shrink true}}
       :onChange       #(state/update-value [:repository :tag] (-> % .-value) form-value-cursor)
       :placeholder    "Search a tag"})))
;
;(defn- form-image-tag-preloaded [value tags]
;  "Preload ports for services created via swarmpit"
;  (form/comp
;    "IMAGE TAG"
;    (comp/autocomplete {:name          "imageTagAuto"
;                        :key           "imageTagAuto"
;                        :searchText    (:tag value)
;                        :onUpdateInput (fn [v] (state/update-value [:repository :tag] v form-value-cursor))
;                        :onNewRequest  (fn [_] (ports/load-suggestable-ports value))
;                        :dataSource    tags})))

(defn- form-name [value update-form?]
  (comp/text-field
    {:key             "service-name"
     :label           "Service name"
     :variant         "outlined"
     :helperText      "Specify name or leave empty for random"
     :disabled        update-form?
     :fullWidth       true
     :value           value
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:serviceName] (-> % .-target .-value) form-value-cursor)}))

(defn- form-mode [value update-form?]
  (comp/form-control
    {:component "fieldset"}
    (comp/form-label {} "Mode")
    (comp/radio-group
      {:name     "mode"
       :key      "mode"
       :value    value
       :onChange (fn [event]
                   (state/update-value [:mode] (-> event .-target .-value) form-value-cursor))}
      (comp/form-control-label
        {:control  (comp/radio
                     {:name "replicated-mode"
                      :key  "replicated-mode"})
         :disabled update-form?
         :value    "replicated"
         :label    "Replicated"})
      (comp/form-control-label
        {:control  (comp/radio
                     {:name "global-mode"
                      :key  "global-mode"})
         :disabled update-form?
         :value    "global"
         :label    "Global"}))))

(defn- form-replicas [value]
  (comp/text-field
    {:key             "replicas"
     :label           "Replicas"
     :type            "number"
     :variant         "outlined"
     :min             0
     :fullWidth       true
     :required        true
     :value           value
     :InputLabelProps {:shrink true}
     :onChange        (fn [event]
                        (state/update-value [:replicas] (parse-int (-> event .-target .-value)) form-value-cursor))}))

(defn- form-command [value]
  (comp/text-field
    {:key             "command"
     :label           "Command"
     :variant         "outlined"
     :helperText      "The command to be run in the image"
     :fullWidth       true
     :multiline       true
     :value           value
     :InputLabelProps {:shrink true}
     :onChange        (fn [event]
                        (let [value (-> event .-target .-value)]
                          (state/update-value [:command] (when (< 0 (count value)) (str/split value #" ")) form-value-cursor)))}))

(defn tags-handler
  [repository]
  (ajax/get
    (routes/path-for-backend :repository-tags)
    {:params     {:repository repository}
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:tags] response form-state-cursor))
     :on-error   (fn [_])}))

(rum/defc form < rum/reactive [update-form?]
  (let [{:keys [repository serviceName mode replicas command]} (state/react form-value-cursor)
        {:keys [tags tagMenuSuggestions]} (state/react form-state-cursor)]
    (comp/grid
      {:container true
       :spacing   24}
      (comp/grid
        {:item true
         :xs   12
         :sm   6} (form-image (:name repository)))
      (comp/grid
        {:item true
         :xs   12
         :sm   6}
        (form-image-tag repository tags))
      (comp/grid
        {:item true
         :xs   12} (form-name serviceName update-form?))
      (comp/grid
        {:item true
         :xs   12
         :sm   6} (form-mode mode update-form?))
      (when (= "replicated" mode)
        (comp/grid
          {:item true
           :xs   12
           :sm   6} (form-replicas replicas)))
      (comp/grid
        {:item true
         :xs   12} (networks/form))
      (comp/grid
        {:item true
         :xs   12} (form-command (str/join " " command))))))
