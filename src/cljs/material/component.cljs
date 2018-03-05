(ns material.component
  (:refer-clojure :exclude [stepper list])
  (:require [material.factory :as factory]
            [material.validation]
            [sablono.core :refer-macros [html]]))

;;; Theme components

(def theme
  {:palette {:primary1Color      "#437f9d"
             :primary2Color      "#3C728D"
             :primary3Color      "#bdbdbd"
             :accent1Color       "#437f9d"
             :accent2Color       "#f5f5f5"
             :accent3Color       "#9e9e9e"
             :textColor          "#757575"
             :alternateTextColor "#ffffff"
             :canvasColor        "#ffffff"
             :borderColor        "#e0e0e0"
             :disabledColor      (factory/fade "rgba(0, 0, 0, 0.87)" 0.3)
             :pickerHeaderColor  "437f9d"
             :clockCircleColor   (factory/fade "rgba(0, 0, 0, 0.87)" 0.07)
             :shadowColor        "#000000"}})

(defn- mui-theme
  [theme]
  (factory/mui-theme (clj->js theme)))

(defn- mui-theme-provider
  [props comp]
  (factory/mui-theme-provider (clj->js props) comp))

(defn mui
  [comp]
  [:div
   (mui-theme-provider
     {:muiTheme (mui-theme theme)}
     comp)])

;;; Single formsy components

(defn vform
  [props & childs] (factory/vform (clj->js props) childs))

(defn vtext-field
  ([props] (factory/vtext (clj->js props)))
  ([] (factory/vtext nil)))

(defn vauto-complete
  ([props] (factory/vauto-complete (clj->js props)))
  ([] (factory/vauto-complete nil)))

;;; Single material-ui components

(defn auto-complete
  ([props] (factory/auto-complete (clj->js props)))
  ([] (factory/auto-complete nil)))

(defn avatar
  ([props] (factory/avatar (clj->js props)))
  ([] (factory/avatar nil)))

(defn snackbar
  ([props] (factory/snackbar (clj->js props)))
  ([] (factory/snackbar nil)))

(defn toogle
  ([props] (factory/toogle (clj->js props)))
  ([] (factory/toogle nil)))

(defn checkbox
  ([props] (factory/checkbox (clj->js props)))
  ([] (factory/checkbox nil)))

(defn slider
  ([props] (factory/slider (clj->js props)))
  ([] (factory/slider nil)))

(defn linear-progress
  ([props] (factory/linear-progress (clj->js props)))
  ([] (factory/linear-progress nil)))

(defn circular-progress
  ([props] (factory/circular-progress (clj->js props)))
  ([] (factory/circular-progress nil)))

(defn refresh-indicator
  ([props] (factory/refresh-indicator (clj->js props)))
  ([] (factory/refresh-indicator nil)))

(defn text-field
  ([props] (factory/text-field (clj->js props)))
  ([] (factory/text-field nil)))

(defn app-bar
  ([props] (factory/app-bar (clj->js props)))
  ([] (factory/app-bar nil)))

(defn menu-item
  ([props] (factory/menu-item (clj->js props)))
  ([] (factory/menu-item nil)))

(defn flat-button
  ([props] (factory/flat-button (clj->js props)))
  ([] (factory/flat-button nil)))

(defn radio-button
  ([props] (factory/radio-button (clj->js props)))
  ([] (factory/radio-button nil)))

(defn raised-button
  ([props] (factory/raised-button (clj->js props)))
  ([] (factory/raised-button nil)))

(defn list-item
  ([props] (factory/list-item (clj->js props)))
  ([] (factory/list-item nil)))

(defn dialog
  [props & childs]
  (factory/dialog (clj->js props) childs))

(defn list
  [props & childs]
  (factory/list (clj->js props) childs))

(defn menu
  [props & childs]
  (factory/menu (clj->js props) childs))

(defn icon-menu
  [props & childs]
  (factory/icon-menu (clj->js props) childs))

(defn icon-button
  [props comp]
  (factory/icon-button (clj->js props) comp))

(defn select-field
  [props & childs]
  (factory/select-field (clj->js props) childs))

(defn drawer
  [props & childs]
  (factory/drawer (clj->js props) childs))

(defn tab
  [props & childs]
  (factory/tab (clj->js props) childs))

(defn tabs
  [props & childs]
  (factory/tabs (clj->js props) childs))

(defn table
  [props & childs]
  (factory/table (clj->js props) childs))

(defn table-header
  [props & childs]
  (factory/table-header (clj->js props) childs))

(defn table-header-column
  [props comp]
  (factory/table-header-column (clj->js props) comp))

(defn table-body
  [props & childs]
  (factory/table-body (clj->js props) childs))

(defn table-row
  [props & childs]
  (factory/table-row (clj->js props) childs))

(defn table-row-column
  ([props comp] (factory/table-row-column (clj->js props) comp))
  ([props] (factory/table-row-column (clj->js props))))

(defn table-footer
  [props & childs]
  (factory/table-footer (clj->js props) childs))

(defn radio-button-group
  [props & childs]
  (factory/radio-button-group (clj->js props) childs))

(defn svg
  ([props d] (factory/svg-icon (clj->js props) (html [:path {:d d}])))
  ([d] (factory/svg-icon nil (html [:path {:d d}]))))

(defn button-icon
  [icon]
  (html [:svg {:width  "18"
               :height "18"
               :fill   "rgb(117, 117, 117)"}
         [:path {:d icon}]]))

;;; Composite components

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