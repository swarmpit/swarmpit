(ns material.component.composite
  (:require [goog.object]
            [material.component :as cmp]
            [sablono.core :refer-macros [html]]
            [clojure.walk :refer [keywordize-keys]]))

(defn progress-button [action action-fn processing?]
  (html
    [:div.Swarmpit-progress-button-wrapper
     (cmp/button
       {:variant  "contained"
        :color    "primary"
        :disabled processing?
        :onClick  action-fn} action)
     (when processing?
       (cmp/circular-progress
         {:size      24
          :className "Swarmpit-progress-button"}))]))

(defn autocomplete-input [props]
  (let [{:keys [inputRef] :as p} (keywordize-keys (js->clj props))]
    (html
      [:div (merge {:ref                   inputRef
                    :backspaceRemovesValue true}
                   (dissoc p :inputRef))])))

(defn autocomplete-single-value [props]
  (let [{:keys [innerProps children]} (keywordize-keys (js->clj props))]
    (cmp/typography innerProps children)))

(defn autocomplete-value-container [props]
  (html
    [:div
     {:style
      {:display    "flex"
       :flexWrap   "wrap"
       :flex       1
       :alignItems "center",
       :overflow   "hidden"}} (goog.object/get props "children")]))

(defn autocomplete-control [props]
  (let [{:keys [innerRef innerProps children selectProps]} (keywordize-keys (js->clj props))]

    (js/console.log selectProps)


    (cmp/text-field
      (merge (:textFieldProps selectProps)
             {:fullWidth  true
              :variant    "outlined"
              :margin     "normal"
              :InputProps {:inputComponent autocomplete-input
                           :inputProps     (merge innerProps
                                                  {:style    {:display "flex"
                                                              :padding "10px 5px"}
                                                   :inputRef innerRef
                                                   :children children})}}))))

(defn autocomplete-option [props]
  (let [{:keys [innerRef isFocused children isSelected innerProps]} (keywordize-keys (js->clj props))]
    (cmp/menu-item
      (merge innerProps
             {:buttonRef innerRef
              :selected  isFocused
              :component "div"
              :style     {:fontWeight (if isSelected 500 400)}}) children)))

(defn autocomplete [props]
  (cmp/no-ssr
    {}
    (cmp/react-select
      (merge props
             {:components {:Control        autocomplete-control
                           :Option         autocomplete-option
                           :ValueContainer autocomplete-value-container
                           :SingleValue    autocomplete-single-value
                           }}))))