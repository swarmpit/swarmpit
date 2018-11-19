(ns material.component.list.info
  (:require [material.icon :as icon]
            [material.component :as cmp]
            [material.component.list.util :refer [primary-key render-keys render-value?]]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.utils :refer [select-keys*]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(defn table-head
  [render-metadata]
  (cmp/table-head
    {:key "Swarmpit-table-head"}
    (cmp/table-row
      {:key       "Swarmpit-table-head-row"
       :className "Swarmpit-table-info-row"}
      (map-indexed
        (fn [index header]
          (cmp/table-cell
            {:key       (str "Swarmpit-table-head-cell-" index)
             :className "Swarmpit-table-head-cell Swarmpit-table-row-cell-info"}
            (:name header))) render-metadata))))

(defn table-body
  [render-metadata items onclick-handler-fn]
  (cmp/table-body
    {:key "Swarmpit-table-body"}
    (map-indexed
      (fn [index item]
        (cmp/table-row
          (merge {:key       (str "Swarmpit-table-row-" index)
                  :hover     true
                  :className "Swarmpit-table-info-row"}
                 (when onclick-handler-fn
                   {:onClick #(onclick-handler-fn item)}))
          (->> (select-keys* item (render-keys render-metadata))
               (map-indexed
                 (fn [coll-index coll]
                   (cmp/table-cell
                     {:key       (str "Swarmpit-table-row-cell-" index "-" coll-index)
                      :className "Swarmpit-table-row-cell-info"}
                     (val coll))))))) items)))

(defn table
  [render-metadata items onclick-handler-fn]
  (cmp/table
    {:key       "Swarmpit-table"
     :className "Swarmpit-table Swarmpit-table-card"}
    (table-head render-metadata)
    (table-body render-metadata items onclick-handler-fn)))
