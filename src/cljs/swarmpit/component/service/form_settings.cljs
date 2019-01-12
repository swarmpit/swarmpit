(ns swarmpit.component.service.form-settings
  (:require [material.components :as comp]
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
     :style           {:maxWidth "350px"}
     :fullWidth       true
     :disabled        true
     :required        true
     :defaultValue    value
     :InputLabelProps {:shrink true}}))

(defn- form-image-tag [value tags]
  "For update services there is no port preload"
  (let [suggestions (map #(hash-map :label %
                                    :value %) tags)]
    (composite/autocomplete
      {:options        suggestions
       :textFieldProps {:label           "Tag"
                        :key             "tag"
                        :margin          "normal"
                        :style           {:maxWidth "350px"}
                        :helperText      "Specify image tag or leave empty for latest"
                        :InputLabelProps {:shrink true}}
       :onChange       #(state/update-value [:repository :tag] (-> % .-value) form-value-cursor)
       :value          {:label (:tag value)
                        :value (:tag value)}
       :placeholder    "Search a tag"})))

(defn- form-image-tag-preloaded [value tags]
  "Preload ports for services created via swarmpit"
  (let [suggestions (map #(hash-map :label %
                                    :value %) tags)]
    (composite/autocomplete
      {:options        suggestions
       :textFieldProps {:label           "Tag"
                        :key             "tag"
                        :margin          "normal"
                        :style           {:maxWidth "350px"}
                        :helperText      "Specify image tag or leave empty for latest"
                        :InputLabelProps {:shrink true}}
       :onChange       (fn [v]
                         (state/update-value [:repository :tag] (-> v .-value) form-value-cursor)
                         (ports/load-suggestable-ports (merge value
                                                              {:tag (-> v .-value)})))
       :value          {:label (:tag value)
                        :value (:tag value)}
       :placeholder    "Search a tag"})))

(defn- form-name [value update-form?]
  (comp/text-field
    {:key             "service-name"
     :label           "Service name"
     :variant         "outlined"
     :style           {:maxWidth "350px"}
     :helperText      "Specify name or leave empty for random"
     :disabled        update-form?
     :fullWidth       true
     :defaultValue    value
     :InputLabelProps {:shrink true}
     :onChange        #(state/update-value [:serviceName] (-> % .-target .-value) form-value-cursor)}))

(defn- form-mode [value update-form?]
  (comp/form-control
    {:component "fieldset"
     :key       "modef"
     :style     {:width "200px"}}
    (comp/form-label
      {:key "model"} "Mode")
    (comp/radio-group
      {:name     "mode"
       :key      "mode"
       :value    value
       :onChange (fn [event]
                   (state/update-value [:mode] (-> event .-target .-value) form-value-cursor))}
      (comp/form-control-label
        {:control  (comp/radio
                     {:name  "replicated-mode"
                      :color "primary"
                      :key   "replicated-mode"})
         :disabled update-form?
         :key      "repl-mode"
         :value    "replicated"
         :label    "Replicated"})
      (comp/form-control-label
        {:control  (comp/radio
                     {:name  "global-mode"
                      :color "primary"
                      :key   "global-mode"})
         :disabled update-form?
         :key      "glob-mode"
         :value    "global"
         :label    "Global"}))))

(defn- form-replicas [value]
  (comp/text-field
    {:key             "replicas"
     :label           "Replicas"
     :type            "number"
     :variant         "outlined"
     :style           {:maxWidth "150px"}
     :min             0
     :fullWidth       true
     :required        true
     :margin          "dense"
     :defaultValue    value
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
     :defaultValue    value
     :InputProps      {:style {:fontFamily "monospace"}}
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
        {:keys [tags]} (state/react form-state-cursor)]
    (comp/grid
      {:container true
       :key       "sfscg"
       :spacing   24}
      (comp/grid
        {:item true
         :key  "sfscgii"
         :xs   12
         :sm   6}
        (form-image (:name repository)))
      (comp/grid
        {:item true
         :key  "sfscgit"
         :xs   12
         :sm   6}
        (if update-form?
          (form-image-tag repository tags)
          (form-image-tag-preloaded repository tags)))
      (comp/grid
        {:item true
         :key  "sfscgin"
         :xs   12}
        (form-name serviceName update-form?))
      (comp/grid
        {:item true
         :key  "sfscgimar"
         :xs   12
         :sm   6}
        (form-mode mode update-form?)
        (when (= "replicated" mode)
          (form-replicas replicas)))
      (comp/grid
        {:item true
         :key  "sfscgicomm"
         :xs   12}
        (form-command (str/join " " command))))))
