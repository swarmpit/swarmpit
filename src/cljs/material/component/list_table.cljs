(ns material.component.list-table
  (:refer-clojure :exclude [filter])
  (:require [material.component :as cmp]
            [material.icon :as icon]
            [swarmpit.utils :refer [select-keys* map-values]]
            [swarmpit.url :refer [dispatch!]]))

(defn table-header
  [headers]
  (cmp/table-header
    {:key               "th"
     :displaySelectAll  false
     :adjustForCheckbox false
     :style             {:border "none"}}
    (cmp/table-row
      {:key           "tr"
       :displayBorder true}
      (map-indexed
        (fn [index header]
          (cmp/table-header-column
            {:key   (str "thc-" index)
             :style (select-keys header [:width])}
            (:name header))) headers))))

(defn table-body
  [headers items render-item-fn render-items-key]
  (cmp/table-body
    {:key                "tb"
     :showRowHover       true
     :displayRowCheckbox false}
    (map-indexed
      (fn [index item]
        (cmp/table-row
          {:key       (str "tr-" (hash item))
           :style     {:cursor "pointer"}
           :rowNumber index}
          (->> (select-keys* item render-items-key)
               (map-indexed
                 (fn [coll-index coll]
                   (cmp/table-row-column
                     {:key   (str "trc-" index "-" coll-index)
                      :style (select-keys (nth headers coll-index) [:width])}
                     (render-item-fn coll item))))))) items)))

(defn table-paging
  [offset total limit on-prev-fn on-next-fn]
  (cmp/table-footer
    {:key "tf"}
    (cmp/table-row
      {:key "tfr"}
      (cmp/table-row-column
        {:key      "tfrcbtn"
         :style    {:float "right"}
         :children [(cmp/icon-button
                      {:key      "tfrcibprev"
                       :disabled (= offset 0)
                       :onClick  #(on-prev-fn)}
                      (cmp/svg icon/left))
                    (cmp/icon-button
                      {:key      "tfrcibnext"
                       :disabled (< total
                                    (+ offset limit))
                       :onClick  #(on-next-fn)}
                      (cmp/svg icon/right))]})
      (cmp/table-row-column
        {:key   "tfrcinf"
         :style {:float      "right"
                 :paddingTop 16
                 :height     16}}
        (str (min (+ offset 1) total) " - "
             (min (+ offset limit) total) " of " total)))))

(defn- table-loading [loading]
  (let [mode (if loading "indeterminate"
                         "determinate")]
    (cmp/mui
      (cmp/linear-progress
        {:mode  mode
         :style {:borderRadius 0
                 :background   "rgb(224, 228, 231)"
                 :height       "1px"
                 :position     "relative"
                 :top          "59px"}}))))

(defn table
  ([headers items render-item-fn render-items-key onclick-handler-fn]
   (table headers items false render-item-fn render-items-key onclick-handler-fn))
  ([headers items loading? render-item-fn render-items-key onclick-handler-fn]
   (let [item (fn [index] (nth items index))]
     [:div
      (table-loading loading?)
      (cmp/mui
        (cmp/table
          {:key         "tbl"
           :selectable  false
           :onCellClick (fn [i]
                          (dispatch!
                            (onclick-handler-fn (item i))))}
          (table-header headers)
          (table-body headers
                      items
                      render-item-fn
                      render-items-key)))])))

(defn filter
  [items query]
  (if (or (empty? query)
          (< (count query) 2))
    items
    (clojure.core/filter
      (fn [item]
        (->> (map-values item)
             (clojure.core/filter #(clojure.string/includes? % query))
             (empty?)
             (not))) items)))
