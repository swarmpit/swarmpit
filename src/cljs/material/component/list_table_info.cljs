(ns material.component.list-table-info
  (:require [material.component :as cmp]
            [swarmpit.utils :refer [select-keys*]]))

(defn table-header [headers]
  (cmp/table-header
    {:key               "th"
     :displaySelectAll  false
     :adjustForCheckbox false
     :style             {:border "none"}}
    (cmp/table-row
      {:key           "tr"
       :displayBorder false
       :style         {:height "20px"}}
      (map-indexed
        (fn [index header]
          (cmp/table-header-column
            {:key   (str "thc-" index)
             :style {:height "20px"}}
            (:name header))) headers)
      (cmp/table-header-column
        {:key   "thc"
         :style {:height "20px"
                 :width  "100%"}} ""))))

(defn table-body [items render-item-fn render-items-key]
  (cmp/table-body
    {:key                "tb"
     :showRowHover       true
     :displayRowCheckbox false}
    (map-indexed
      (fn [index item]
        (cmp/table-row
          {:key           (str "tr-" index)
           :rowNumber     index
           :displayBorder false
           :style         {:height "20px"}}
          (->> (select-keys* item render-items-key)
               (map-indexed
                 (fn [coll-index coll]
                   (cmp/table-row-column
                     {:key   (str "trc-" index "-" coll-index)
                      :style {:height "20px"}}
                     (render-item-fn coll item)))))
          (cmp/table-header-column
            {:key   (str "trc-" index)
             :style {:height "20px"
                     :width  "100%"}} ""))) items)))

(defn table [headers items render-item-fn render-items-key]
  (cmp/mui
    (cmp/table
      {:key   "tbl"
       :style {:tableLayout "auto"}}
      (table-header headers)
      (table-body items render-item-fn render-items-key))))

(defn table-headless [items render-item-fn render-items-key]
  (cmp/mui
    (cmp/table
      {:key   "tbl"
       :style {:tableLayout "auto"}}
      (table-body items render-item-fn render-items-key))))