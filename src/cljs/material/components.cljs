(ns material.components
  (:refer-clojure :exclude [stepper list])
  (:require [material.factory :as f]
            [sablono.core :refer-macros [html]]))

;;; Theme components

(def theme-props
  {:palette    {:primary   {:main         "#65519f"
                            :light        "#957ed1"
                            :dark         "#362870"
                            :contrastText "#fff"}
                :secondary {:main         "#65519f"
                            :light        "#957ed1"
                            :dark         "#362870"
                            :contrastText "#fff"}}
   :typography {:useNextVariants true}})

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

(defn form-label
  [props label]
  (f/form-label (clj->js props) label))

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
  [props & childs]
  (f/menu-item (clj->js props) childs))

(defn menu-list
  [props & childs]
  (f/menu-list (clj->js props) childs))

(defn paper
  [props & childs]
  (f/paper (clj->js props) childs))

(defn dialog
  [props & childs]
  (f/dialog (clj->js props) childs))

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
  [props & childs]
  (f/avatar (clj->js props) childs))

(defn chip
  [props]
  (f/chip (clj->js props)))

(defn list
  [props & childs]
  (f/list (clj->js props) childs))

(defn list-subheader
  [props & childs]
  (f/list-subheader (clj->js props) childs))

(defn list-item
  [props & childs]
  (f/list-item (clj->js props) childs))

(defn list-item-icon
  [props icon]
  (f/list-item-icon (clj->js props) icon))

(defn list-item-text
  [props]
  (f/list-item-text (clj->js props)))

(defn list-item-secondary-action
  [props & childs]
  (f/list-item-secondary-action (clj->js props) childs))

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
  [props & childs]
  (f/typography (clj->js props) childs))

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
  [props & childs]
  (f/expansion-panel-summary (clj->js props) childs))

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

(defn card-actions
  [props & childs]
  (f/card-actions (clj->js props) childs))

(defn card-media
  [props]
  (f/card-media (clj->js props)))

(defn checkbox
  ([props] (f/checkbox (clj->js props)))
  ([] (f/checkbox nil)))

(defn stepper
  [props & childs]
  (f/stepper (clj->js props) childs))

(defn step
  [props & childs]
  (f/step (clj->js props) childs))

(defn step-label
  [props label]
  (f/step-label (clj->js props) label))

(defn step-content
  [props & childs]
  (f/step-content (clj->js props) childs))

(defn select
  [props & childs]
  (f/select (clj->js props) childs))

(defn tooltip
  [props comp]
  (f/tooltip (clj->js props) comp))

(defn snackbar
  [props content]
  (f/snackbar (clj->js props) content))

(defn snackbar-content
  ([props] (f/snackbar-content (clj->js props)))
  ([] (f/snackbar-content nil)))

(defn text-field
  ([props & childs] (f/text-field (clj->js props) childs))
  ([] (f/text-field nil)))

(defn tab
  [props]
  (f/tab (clj->js props)))

(defn tabs
  [props & childs]
  (f/tabs (clj->js props) childs))

(defn popper
  [props grow-fn]
  (f/popper (clj->js props) grow-fn))

(defn grow
  [props comp]
  (f/grow (clj->js props) comp))

(defn fade
  [props comp]
  (f/fade (clj->js props) comp))

(defn no-ssr
  [props & childs]
  (f/no-ssr (clj->js props) childs))

(defn radio
  ([props] (f/radio (clj->js props)))
  ([] (f/radio nil)))

(defn radio-group
  [props & childs]
  (f/radio-group (clj->js props) childs))

(defn switch
  [props & childs]
  (f/switch (clj->js props) childs))

(defn linear-progress
  ([props] (f/linear-progress (clj->js props)))
  ([] (f/linear-progress nil)))

(defn circular-progress
  ([props] (f/circular-progress (clj->js props)))
  ([] (f/circular-progress nil)))

(defn click-away-listener
  [props content]
  (f/click-away-listener (clj->js props) content))

;;; Single recharts components

(defn pie-chart
  [props & childs]
  (f/pie-chart (clj->js props) childs))

(defn tooltip-chart
  [props]
  (f/tooltip-chart (clj->js props)))

(defn pie
  [props & childs]
  (f/pie (clj->js props) childs))

(defn cell
  [props]
  (f/cell (clj->js props)))

(defn legend
  [props]
  (f/legend (clj->js props)))

(defn re-label
  [props label]
  (f/label (clj->js props) label))

(defn responsive-container
  [props comp]
  (f/responsive-container (clj->js props) comp))

;;; Single react components

(defn react-select
  [props]
  (f/react-select (clj->js props)))

(defn react-autosuggest
  [props]
  (f/react-autosuggest (clj->js props)))

(defn rc-slider
  [props]
  (f/rc-slider (clj->js props)))

(defn rc-slider-handle
  [props]
  (f/rc-slider-handle (clj->js props)))
