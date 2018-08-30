(ns material.component
  (:refer-clojure :exclude [stepper list])
  (:require [material.factory :as f]
            [material.validation]
            [sablono.core :refer-macros [html]]))

;;; Theme components

(def theme-props
  {:palette {:primary   {:main "#65519f"}
             :secondary {:main "rgb(117, 117, 117)"}}})

(def theme (f/create-mui-theme (clj->js theme-props)))

(defn mui [component]
  (f/mui-theme-provider (clj->js {:theme theme}) (f/css-baseline) component))

;;; Single material-ui components

(defn divider
  ([props] (f/divider (clj->js props)))
  ([] (f/divider nil)))

(defn form-control
  [props & childs]
  (f/form-control (clj->js props) childs))

(defn form-control-label
  [props]
  (f/form-control-label (clj->js props)))

(defn form-group
  [props & childs]
  (f/form-group (clj->js props) childs))

(defn form-helper-text
  [props & childs]
  (f/form-helper-text (clj->js props) childs))

(defn toolbar
  [props & childs]
  (f/toolbar (clj->js props) childs))

(defn drawer
  [props & childs]
  (f/drawer (clj->js props) childs))

(defn menu
  [props & childs]
  (f/menu (clj->js props) childs))

(defn menu-item
  [props label]
  (f/menu-item (clj->js props) label))

(defn paper
  [props & childs]
  (f/paper (clj->js props) childs))

(defn input
  [props]
  (f/input (clj->js props)))

(defn input-adornment
  [props adorment]
  (f/input-adornment (clj->js props) adorment))

(defn input-label
  [props label]
  (f/input-label (clj->js props) label))

(defn avatar
  [props]
  (f/avatar (clj->js props)))

(defn chip
  [props]
  (f/chip (clj->js props)))

(defn list-item
  [props & childs]
  (f/list-item (clj->js props) childs))

(defn list-item-icon
  [props icon]
  (f/list-item-icon (clj->js props) icon))

(defn list-item-text
  [props]
  (f/list-item-text (clj->js props)))

(defn icon-button
  [props icon]
  (f/icon-button (clj->js props) icon))

(defn button
  [props & childs]
  (f/button (clj->js props) childs))

(defn hidden
  [props comp]
  (f/hidden (clj->js props) comp))

(defn app-bar
  [props drawer]
  (f/appbar (clj->js props) drawer))

(defn typography
  [props label]
  (f/typography (clj->js props) label))

(defn svg
  ([props d] (f/svg-icon (clj->js props) (html [:path {:d d}])))
  ([d] (f/svg-icon nil (html [:path {:d d}]))))

(defn table
  [props & childs]
  (f/table (clj->js props) childs))

(defn table-head
  [props & childs]
  (f/table-head (clj->js props) childs))

(defn table-cell
  [props comp]
  (f/table-cell (clj->js props) comp))

(defn table-body
  [props & childs]
  (f/table-body (clj->js props) childs))

(defn table-row
  [props & childs]
  (f/table-row (clj->js props) childs))

(defn table-footer
  [props & childs]
  (f/table-footer (clj->js props) childs))

(defn expansion-panel
  [props & childs]
  (f/expansion-panel (clj->js props) childs))

(defn expansion-panel-details
  [props comp]
  (f/expansion-panel-details (clj->js props) comp))

(defn expansion-panel-summary
  [props comp]
  (f/expansion-panel-summary (clj->js props) comp))

(defn expansion-panel-actions
  [props & childs]
  (f/expansion-panel-actions (clj->js props) childs))

(defn grid
  [props & childs]
  (f/grid (clj->js props) childs))

(defn card
  [props & childs]
  (f/card (clj->js props) childs))

(defn card-header
  [props]
  (f/card-header (clj->js props)))

(defn card-content
  [props & childs]
  (f/card-content (clj->js props) childs))

(defn checkbox
  ([props] (f/checkbox (clj->js props)))
  ([] (f/checkbox nil)))

(defn select
  [props & childs]
  (f/select (clj->js props) childs))

(defn tooltip
  [props comp]
  (f/tooltip (clj->js props) comp))


















(defn auto-complete
  ([props] (f/auto-complete (clj->js props)))
  ([] (f/auto-complete nil)))

(defn snackbar
  ([props] (f/snackbar (clj->js props)))
  ([] (f/snackbar nil)))

(defn toogle
  ([props] (f/toogle (clj->js props)))
  ([] (f/toogle nil)))

(defn slider
  ([props] (f/slider (clj->js props)))
  ([] (f/slider nil)))

(defn linear-progress
  ([props] (f/linear-progress (clj->js props)))
  ([] (f/linear-progress nil)))

(defn circular-progress
  ([props] (f/circular-progress (clj->js props)))
  ([] (f/circular-progress nil)))

(defn refresh-indicator
  ([props] (f/refresh-indicator (clj->js props)))
  ([] (f/refresh-indicator nil)))

(defn text-field
  ([props] (f/text-field (clj->js props)))
  ([] (f/text-field nil)))

(defn flat-button
  ([props] (f/flat-button (clj->js props)))
  ([] (f/flat-button nil)))

(defn radio-button
  ([props] (f/radio-button (clj->js props)))
  ([] (f/radio-button nil)))

(defn raised-button
  ([props & childs] (f/raised-button (clj->js props) childs))
  ([props] (f/raised-button (clj->js props)))
  ([] (f/raised-button nil)))














(defn dialog
  [props & childs]
  (f/dialog (clj->js props) childs))



(defn icon-menu
  [props & childs]
  (f/icon-menu (clj->js props) childs))

(defn select-field
  [props & childs]
  (f/select-field (clj->js props) childs))

(defn tab
  [props & childs]
  (f/tab (clj->js props) childs))

(defn tabs
  [props & childs]
  (f/tabs (clj->js props) childs))

(defn radio-button-group
  [props & childs]
  (f/radio-button-group (clj->js props) childs))

(defn button-icon
  [icon]
  (html [:svg {:width  "18"
               :height "18"
               :fill   "rgb(117, 117, 117)"}
         [:path {:d icon}]]))

;;; Composite components

;(defn vtextfield [props]
;  (vtext-field
;    (merge props
;           {:required      true
;            :underlineShow false
;            :inputStyle    {:color "rgb(117, 117, 117)"}})))

(defn loader [props]
  (refresh-indicator
    (merge props
           {:size  30
            :left  8
            :style {:display  "inline-block"
                    :position "relative"}})))

(defn autocomplete [props]
  (auto-complete
    (merge props
           {:listStyle {:overflow-y "scroll"
                        :maxHeight  "230px"}})))

(defn progress-button [props progress?]
  (let [disabled? (:disabled props)]
    [:div {:style {:position "relative"}}
     (mui
       (raised-button
         (assoc props :disabled (or disabled? progress?))))
     (when progress?
       (mui
         (circular-progress {:size  24
                             :style {:zIndex     1000
                                     :position   "absolute"
                                     :top        "50%"
                                     :left       "50%"
                                     :marginTop  -12
                                     :marginLeft -12}})))]))