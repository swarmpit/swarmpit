(ns material.component.list-table-form
  (:require [material.component :as cmp]
            [material.icon :as icon]))

(defn textfield
  [props]
  (cmp/text-field
    (merge props
           {:style {:width "100%"}})))

(defn selectfield
  [props & childs]
  (cmp/select-field
    (merge props
           {:style      {:top   7
                         :width "100%"}
            :labelStyle {:lineHeight "45px"
                         :top        2}}) childs))

(defn table-header
  [headers]
  (cmp/table-header
    {:key               "th"
     :displaySelectAll  false
     :adjustForCheckbox false
     :style             {:border "none"}}
    (cmp/table-row
      {:key           "tr"
       :displayBorder false
       :style         {:height "35px"}}
      (map-indexed
        (fn [index header]
          (cmp/table-header-column
            {:key   (str "thc-" index)
             :style (merge (select-keys header [:width])
                           {:height "35px"})}
            (:name header))) headers)
      (cmp/table-header-column
        {:key   "thc"
         :style {:height "35px"}} ""))))

(defn table-body
  [headers items data render-items-fn remove-item-fn]
  (cmp/table-body
    {:key                "tb"
     :displayRowCheckbox false}
    (map-indexed
      (fn [index item]
        (cmp/table-row
          {:key           (str "tr-" index)
           :rowNumber     index
           :displayBorder false}
          (map-indexed
            (fn [coll-index coll]
              (cmp/table-row-column
                {:name  (str "trc-" index "-" coll-index)
                 :key   (str "trc-" index "-" coll-index)
                 :style (select-keys (nth headers coll-index) [:width])}
                coll))
            (render-items-fn item index data))
          (cmp/table-row-column
            {:key (str "trc-" index)}
            (cmp/icon-button
              {:onClick #(remove-item-fn index)}
              (cmp/svg
                {:hoverColor "rgb(244, 67, 54)"}
                icon/trash))))) items)))

(defn table
  [headers items data render-items-fn remove-item-fn]
  (cmp/mui
    (cmp/table
      {:key        "tbl"
       :selectable false}
      (table-header headers)
      (table-body headers items data render-items-fn remove-item-fn))))

(defn table-headless
  [headers items data render-items-fn remove-item-fn]
  (cmp/mui
    (cmp/table
      {:key        "tbl"
       :selectable false}
      (table-body headers items data render-items-fn remove-item-fn))))
