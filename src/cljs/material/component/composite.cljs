(ns material.component.composite
  (:require [goog.object :as gobject]
            [material.icon :as icon]
            [material.components :as cmp]
            [sablono.core :refer-macros [html]]))

(set! *warn-on-infer* true)

(defn progress-button
  ([action action-fn processing?]
   (progress-button action action-fn processing? false))
  ([action action-fn processing? disabled?]
   (html
     [:div.Swarmpit-progress-button-wrapper
      (cmp/button
        {:variant  "contained"
         :color    "primary"
         :disabled (or processing? disabled?)
         :onClick  action-fn} action)
      (when processing?
        (cmp/circular-progress
          {:size      24
           :className "Swarmpit-progress-button"}))])))

(defn autocomplete-input [props]
  (let [{:keys [inputRef] :as p} (js->clj props :keywordize-keys true)]
    (html
      [:div (merge {:ref inputRef}
                   (dissoc p :inputRef))])))

(defn autocomplete-no-options-mssg [props]
  (let [{:keys [innerProps children]} (js->clj props :keywordize-keys true)]
    (cmp/typography
      (merge innerProps
             {:color "textSecondary"
              :style {:padding "8px 16px"}}) children)))

(defn autocomplete-placeholder [props]
  (let [{:keys [innerProps children]} (js->clj props :keywordize-keys true)]
    (cmp/typography
      (merge innerProps
             {:color "textSecondary"
              :style {:position "absolute"
                      :left     2
                      :fontSize 16
                      :padding  "10px 15px"}}) children)))

(defn autocomplete-single-value [props]
  (let [{:keys [children]} (js->clj props :keywordize-keys true)]
    (cmp/typography
      {:style {:fontSize 16}} children)))

(defn autocomplete-multi-value [props]
  (let [{:keys [removeProps children]} (js->clj props :keywordize-keys true)]
    (cmp/chip
      {:tabIndex   -1
       :onDelete   (:onClick removeProps)
       :deleteIcon (icon/cancel removeProps)
       :style      {:marginRight "5px"}
       :color      "primary"
       :variant    "outlined"
       :label      children})))

(defn autocomplete-menu [props]
  (cmp/paper
    (js/Object.assign (gobject/get props "innerProps")
                      #js {:key   "acm"
                           :style #js {:position  "absolute"
                                       :zIndex    2
                                       :marginTop 8
                                       :left      0
                                       :right     0}})
    (gobject/get props "children")))

(defn autocomplete-value-container [props]
  (let [childs (gobject/get props "children")]
    (html
      [:div
       {:style {:display    "flex"
                :flexWrap   "wrap"
                :flex       1
                :alignItems "center"
                :overflow   "hidden"}} childs])))

(defn autocomplete-control [props]
  (let [{:keys [innerRef innerProps children selectProps]} (js->clj props :keywordize-keys true)
        margin (get-in selectProps [:textFieldProps :margin])]
    (cmp/text-field
      (merge (:textFieldProps selectProps)
             {:fullWidth  true
              :variant    "outlined"
              :InputProps {:inputComponent autocomplete-input
                           :inputProps     (merge innerProps
                                                  {:style    (merge
                                                               {:display "flex"}
                                                               (when (= "normal" margin)
                                                                 {:padding "10px 15px"})
                                                               (when (= "dense" margin)
                                                                 {:padding "6.5px 15px"}))
                                                   :key      "input-key"
                                                   :inputRef innerRef
                                                   :children children})}}))))

(defn autocomplete-option [props]
  (cmp/menu-item
    (js/Object.assign
      (gobject/get props "innerProps")
      #js {:buttonRef (gobject/get props "innerRef")
           :selected  (gobject/get props "isFocused")
           :component "div"
           :style     #js {:fontWeight (if (gobject/get props "isSelected") 500 400)}})
    (gobject/get props "children")))

(defn autocomplete [props]
  (cmp/no-ssr
    {}
    (cmp/react-select
      (merge
        props
        {:components
         {:Control          autocomplete-control
          :Option           autocomplete-option
          :ValueContainer   autocomplete-value-container
          :SingleValue      autocomplete-single-value
          :MultiValue       autocomplete-multi-value
          :Placeholder      autocomplete-placeholder
          :NoOptionsMessage autocomplete-no-options-mssg
          :Menu             autocomplete-menu}}))))