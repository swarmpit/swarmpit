(ns material.component.list-table-auto
  (:require [material.component :as cmp]
            [swarmpit.utils :refer [select-keys*]]
            [swarmpit.url :refer [dispatch!]]))

(defn table-header
  [headers]
  (cmp/table-header
    {:key               "th"
     :style             {:border "none"}
     :displaySelectAll  false
     :adjustForCheckbox false}
    (cmp/table-row
      {:key           "tr"
       :style         {:height "30px"}
       :displayBorder false}
      (map-indexed
        (fn [index header]
          (cmp/table-header-column
            {:key   (str "thc-" index)
             :style {:height "30px"}}
            header)) headers)
      (cmp/table-header-column
        {:key   "thc"
         :style {:height "30px"
                 :width  "100%"}} ""))))

(defn table-body
  [items clickable? render-item-fn render-items-key]
  (cmp/table-body
    {:key                "tb"
     :showRowHover       true
     :displayRowCheckbox false}
    (map-indexed
      (fn [index item]
        (cmp/table-row
          {:key           (str "tr-" (hash item))
           :style         (merge
                            {:height "30px"}
                            (when clickable? {:cursor "pointer"}))
           :displayBorder false
           :rowNumber     index}
          (->> (select-keys* item render-items-key)
               (map-indexed
                 (fn [coll-index coll]
                   (cmp/table-row-column
                     {:key   (str "trc-" index "-" coll-index)
                      :style {:height "30px"}}
                     (render-item-fn coll item)))))
          (cmp/table-row-column
            {:key   (str "trc-" index)
             :style {:height "30px"
                     :width  "100%"}} ""))) items)))

(defn table
  [headers items render-item-fn render-items-key onclick-handler-fn]
  (let [item (fn [index] (nth items index))]
    (cmp/mui
      (cmp/table
        {:key         "tbl"
         :fixedHeader false
         :style       {:tableLayout "auto"}
         :selectable  false
         :onCellClick (fn [i]
                        (when (some? onclick-handler-fn)
                          (dispatch!
                            (onclick-handler-fn (item i)))))}
        (table-header headers)
        (table-body items
                    (some? onclick-handler-fn)
                    render-item-fn
                    render-items-key)))))
