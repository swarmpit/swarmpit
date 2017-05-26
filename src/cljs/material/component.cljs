(ns material.component
  (:refer-clojure :exclude [stepper])
  (:require [material.factory :as factory]
            [material.icon :as icon]
            [sablono.core :refer-macros [html]]
            [swarmpit.uri :refer [dispatch!]]
            [swarmpit.utils :refer [select-keys*]]))

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

;;; Single components

(defn avatar
  ([props] (factory/avatar (clj->js props)))
  ([] (factory/avatar nil)))

(defn snackbar
  ([props] (factory/snackbar (clj->js props)))
  ([] (factory/snackbar nil)))

(defn stepper
  ([props] (factory/stepper (clj->js props)))
  ([] (factory/stepper nil)))

(defn toogle
  ([props] (factory/toogle (clj->js props)))
  ([] (factory/toogle nil)))

(defn checkbox
  ([props] (factory/checkbox (clj->js props)))
  ([] (factory/checkbox nil)))

(defn slider
  ([props] (factory/slider (clj->js props)))
  ([] (factory/slider nil)))

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

(defn radio-button
  ([props] (factory/radio-button (clj->js props)))
  ([] (factory/radio-button nil)))

(defn raised-button
  ([props] (factory/raised-button (clj->js props)))
  ([] (factory/raised-button nil)))

(defn chip
  [props & childs] (factory/chip (clj->js props) childs))

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

(defn step
  [props & childs]
  (factory/step (clj->js props) childs))

(defn step-button
  [props & childs]
  (factory/step-button (clj->js props) childs))

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
  [props comp]
  (factory/table-row-column (clj->js props) comp))

(defn radio-button-group
  [props & childs]
  (factory/radio-button-group (clj->js props) childs))

(defn svg
  ([props d] (factory/svg-icon (clj->js props) (html [:path {:d d}])))
  ([d] (factory/svg-icon nil (html [:path {:d d}]))))

;;; Composite components

(defn panel-text-field
  [props]
  (mui
    (text-field
      (merge props
             {:underlineStyle {:borderColor "rgba(0, 0, 0, 0.2)"}
              :style          {:height     "44px"
                               :lineHeight "15px"}}))))

(defn panel-comp [label comp]
  [:div.form-panel-item
   [:div.form-panel-item-field (mui comp)]
   [:span.form-panel-item-label label]])

;; Labels

(defn label-red
  [text]
  (html [:span.label.label-red text]))

(defn label-yellow
  [text]
  (html [:span.label.label-yellow text]))

(defn label-green
  [text]
  (html [:span.label.label-green text]))

(defn label-blue
  [text]
  (html [:span.label.label-blue text]))

(defn label-grey
  [text]
  (html [:span.label.label-grey text]))

;; Form component layout

(defn form-comp [label comp]
  [:div.form-edit-row
   [:span.form-row-label label]
   [:div.form-row-field (mui comp)]])

(defn form-item [label value]
  [:div.form-view-row
   [:span.form-row-label label]
   [:div.form-row-value value]])

(defn form-section [label]
  [:div.form-view-row
   [:span.form-row-section label]])

;; List table component

(defn list-table-header
  [headers]
  (table-header
    {:key               "th"
     :displaySelectAll  false
     :adjustForCheckbox false
     :style             {:border "none"}}
    (table-row
      {:key           "tr"
       :displayBorder true}
      (map-indexed
        (fn [index header]
          (table-header-column
            {:key (str "thc-" index)}
            header))
        headers))))

(defn list-table-body
  [items render-item-fn render-items-key]
  (table-body
    {:key                "tb"
     :showRowHover       true
     :displayRowCheckbox false}
    (map-indexed
      (fn [index item]
        (table-row
          {:key       (str "tr-" (:id item))
           :style     {:cursor "pointer"}
           :rowNumber index}
          (->> (select-keys* item render-items-key)
               (map #(table-row-column
                       {:key (str "trc-" (:id item))}
                       (render-item-fn %))))))
      items)))

(defn list-table
  [headers items render-item-fn render-items-key url]
  (let [item-id (fn [index] (:id (nth items index)))]
    (mui
      (table
        {:key         "tbl"
         :selectable  false
         :onCellClick (fn [i]
                        (dispatch!
                          (str url (item-id i))))}
        (list-table-header headers)
        (list-table-body items
                         render-item-fn
                         render-items-key)))))

;; Form table component

(defn form-table-header
  [headers add-item-fn]
  (table-header
    {:key               "th"
     :displaySelectAll  false
     :adjustForCheckbox false
     :style             {:border "none"}}
    (table-row
      {:key           "tr"
       :displayBorder false}
      (map-indexed
        (fn [index header]
          (table-header-column
            {:key (str "thc-" index)}
            header))
        headers)
      (table-header-column
        {:key "thc"}
        (icon-button
          {:onClick #(add-item-fn)}
          (svg
            {:hoverColor "#437f9d"}
            icon/plus))))))

(defn form-table-body
  [items render-items-fn remove-item-fn]
  (table-body
    {:key                "tb"
     :displayRowCheckbox false}
    (map-indexed
      (fn [index item]
        (table-row
          {:key           (str "tr-" index)
           :rowNumber     index
           :displayBorder false}
          (map (fn [row] row)
               (render-items-fn item index))
          (table-row-column
            {:key (str "trc-" index)}
            (icon-button
              {:onClick #(remove-item-fn index)}
              (svg
                {:hoverColor "red"}
                icon/trash)))))
      items)))

(defn form-table
  [headers items render-items-fn add-item-fn remove-item-fn]
  (mui
    (table
      {:key        "tbl"
       :selectable false}
      (form-table-header headers add-item-fn)
      (form-table-body items render-items-fn remove-item-fn))))

;; Info table component

(defn info-table-header [headers header-el-style]
  (table-header
    {:key               "th"
     :displaySelectAll  false
     :adjustForCheckbox false
     :style             {:border "none"}}
    (table-row
      {:key           "tr"
       :displayBorder false
       :style         header-el-style}
      (map-indexed
        (fn [index header]
          (table-header-column
            {:key   (str "thc-" index)
             :style header-el-style}
            header))
        headers))))

(defn info-table-body [items item-el-style]
  (table-body
    {:key                "tb"
     :showRowHover       false
     :displayRowCheckbox false}
    (map-indexed
      (fn [index item]
        (table-row
          {:key           (str "tr-" index)
           :rowNumber     index
           :displayBorder false
           :style         item-el-style}
          (->> (keys item)
               (map #(table-row-column
                       {:key   (str "trc-" index "-" %)
                        :style item-el-style} (% item))))))
      items)))

(defn info-table [headers items width]
  (let [el-style {:height "20px"}]
    (mui
      (table
        {:key        "tbl"
         :selectable false
         :style      {:width width}}
        (info-table-header headers el-style)
        (info-table-body items el-style)))))

