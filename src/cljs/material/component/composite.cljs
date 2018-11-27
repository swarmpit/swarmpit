(ns material.component.composite
  (:require [goog.object]
            [material.icon :as icon]
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
      [:div (merge {:ref inputRef}
                   (dissoc p :inputRef))])))

(defn autocomplete-no-options-mssg [props]
  (let [{:keys [innerProps children]} (keywordize-keys (js->clj props))]
    (cmp/typography
      (merge innerProps
             {:color "textSecondary"
              :style {:padding "8px 16px"}}) children)))

(defn autocomplete-placeholder [props]
  (let [{:keys [innerProps children]} (keywordize-keys (js->clj props))]
    (cmp/typography
      (merge innerProps
             {:color "textSecondary"
              :style {:position "absolute"
                      :left     2
                      :fontSize 16
                      :padding  "10px 15px"}}) children)))

(defn autocomplete-single-value [props]
  (let [{:keys [children]} (keywordize-keys (js->clj props))]
    (cmp/typography
      {:style {:fontSize 16}} children)))

(defn autocomplete-multi-value [props]
  (let [{:keys [removeProps children]} (keywordize-keys (js->clj props))]
    (cmp/chip
      {:tabIndex   -1
       :onDelete   (:onClick removeProps)
       :deleteIcon (icon/cancel removeProps)
       :label      children})))

(defn autocomplete-menu [props]
  (let [{:keys [innerProps]} (keywordize-keys (js->clj props))]
    (cmp/paper
      (merge innerProps
             {:style {:position  "absolute"
                      :zIndex    2
                      :marginTop 8
                      :left      0
                      :right     0}}) (goog.object/get props "children"))))

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
    (cmp/text-field
      (merge (:textFieldProps selectProps)
             {:fullWidth  true
              :variant    "outlined"
              :margin     "normal"
              :InputProps {:inputComponent autocomplete-input
                           :inputProps     (merge innerProps
                                                  {:style    {:display "flex"
                                                              :padding "10px 15px"}
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
             {:components {:Control          autocomplete-control
                           :Option           autocomplete-option
                           :ValueContainer   autocomplete-value-container
                           :SingleValue      autocomplete-single-value
                           :MultiValue       autocomplete-multi-value
                           :Placeholder      autocomplete-placeholder
                           :NoOptionsMessage autocomplete-no-options-mssg
                           :Menu             autocomplete-menu}}))))