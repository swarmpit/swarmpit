(ns swarmpit.component.service.form-settings
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.component.service.form-ports :as ports]
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
     :fullWidth       true
     :disabled        true
     :required        true
     :value           value
     :InputLabelProps {:shrink true}}))

(defn- form-image-tag-r [value tags tagMenuSuggestions]
  (comp/autosuggest
    {:renderInputComponent        (fn [inputProps]
                                    (let [{:keys [ref] :as p} (keywordize-keys (js->clj inputProps))]
                                      (comp/text-field
                                        (merge (dissoc p :ref)
                                               {:id              "image-tag"
                                                :key             "image-tag"
                                                :label           "Image tag"
                                                :variant         "outlined"
                                                :helperText      "Specify image tag or leave empty for latest"
                                                :fullWidth       true
                                                :InputProps      {:inputRef (fn [node] (ref node))}
                                                :InputLabelProps {:shrink true}}))))
     :suggestions                 tagMenuSuggestions
     :onSuggestionsFetchRequested (fn [_] (state/update-value [:tagMenuSuggestions] (filter #(str/includes? % (:tag value)) tags) form-state-cursor))
     :onSuggestionsClearRequested (fn [] (state/update-value [:tagMenuSuggestions] [] form-state-cursor))
     :getSuggestionValue          identity
     :renderSuggestion            (fn [s props]
                                    (comp/menu-item
                                      {:key      s
                                       :selected (.-isHighlighted props)} s))
     ;:renderSuggestionsContainer  (fn [options]
     ;                               (let [{:keys [children containerProps]} (keywordize-keys (js->clj options))]
     ;                                 (comp/paper (merge containerProps
     ;                                                    {:square true}) children)))
     :theme                       {:container                {:position "relative"}
                                   :suggestionsContainerOpen {:position  "absolute"
                                                              :zIndex    1
                                                              :marginTop 8
                                                              :left      0
                                                              :right     0}
                                   :suggestionsList          {:margin        0
                                                              :padding       0
                                                              :listStyleType "none"}
                                   :suggestion               {:display "block"}}
     :inputProps                  {:value    (:tag value)
                                   :onChange (fn [_ props]
                                               (state/update-value [:repository :tag] (.-newValue props) form-value-cursor))}}))

;(defn- form-image-tag [value tags]
;  "For update services there is no port preload"
;  (form/comp
;    "IMAGE TAG"
;    (comp/autocomplete
;      {:name          "image-tag"
;       :key           "image-tag"
;       :searchText    (:tag value)
;       :onUpdateInput (fn [v] (state/update-value [:repository :tag] v form-value-cursor))
;       :dataSource    tags})))
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
     :disabled        update-form?
     :required        true
     :fullWidth       true
     :value           value
     :InputLabelProps {:shrink true}
     :onChange        (fn [_ v]
                        (state/update-value [:serviceName] v form-value-cursor))}))

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
       :spacing   40}
      (comp/grid
        {:item true
         :xs   12
         :sm   6} (form-image (:name repository)))
      (comp/grid
        {:item true
         :xs   12
         :sm   6} (form-image-tag-r repository tags tagMenuSuggestions))
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
         :xs   12} (form-command (str/join " " command))))))