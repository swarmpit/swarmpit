(ns material.component.list.basic
  (:refer-clojure :exclude [list])
  (:require [material.component :as cmp]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(defn table-head
  [render-metadata]
  (cmp/table-head
    {:key "Swarmpit-table-head"}
    (cmp/table-row
      {:key "Swarmpit-table-head-row"}
      (map-indexed
        (fn [index header]
          (cmp/table-cell
            {:key       (str "Swarmpit-table-head-cell-" index)
             :className "Swarmpit-table-head-cell"}
            (:name header))) (:summary render-metadata)))))

(defn table-body
  [render-metadata items onclick-handler-fn]
  (cmp/table-body
    {:key "Swarmpit-table-body"}
    (map-indexed
      (fn [index item]
        (cmp/table-row
          {:key     (str "Swarmpit-table-row-" index)
           :onClick #(onclick-handler-fn item)
           :hover   true}
          (->> (:summary render-metadata)
               (map-indexed
                 (fn [coll-index coll]
                   (let [render-fn (:render-fn coll)]
                     (cmp/table-cell
                       {:key       (str "Swarmpit-table-row-cell-" index "-" coll-index)
                        :className "Swarmpit-table-row-cell"}
                       (render-fn item)))))))) items)))

(defn table
  [render-metadata items onclick-handler-fn]
  (cmp/table
    {:key       "Swarmpit-table"
     :className "Swarmpit-table"}
    (table-head render-metadata)
    (table-body render-metadata items onclick-handler-fn)))

(defn list-item
  [render-metadata index item last onclick-handler-fn]
  (let [status-fn (:status-fn render-metadata)
        primary-key (:primary render-metadata)
        secodary-key (:secondary render-metadata)]
    (cmp/list-item
      {:key     (str "Swarmpit-list-item-" index)
       :button  true
       :divider (false? (= item last))
       :onClick #(onclick-handler-fn item)}
      (cmp/list-item-text
        (merge
          {:key     (str "Swarmpit-list-item-text-" index)
           :classes {:primary   "Swarmpit-list-item-text-primary"
                     :secondary "Swarmpit-list-item-text-secondary"}
           :primary (primary-key item)}
          (when secodary-key
            {:secondary (secodary-key item)})))
      (when status-fn
        (cmp/list-item-secondary-action
          {:key   (str "Swarmpit-list-status-" index)
           :style {:marginRight "10px"}}
          (status-fn item))))))

(defn list
  [render-metadata items onclick-handler-fn]
  (cmp/list
    {:dense true}
    (map-indexed
      (fn [index item]
        (list-item
          render-metadata
          index
          item
          (last items)
          onclick-handler-fn)) items)))

(rum/defc responsive < rum/reactive
  [render-metadata items onclick-handler-fn]
  (html
    [:div
     (cmp/hidden
       {:only           ["xs" "sm" "md"]
        :implementation "js"}
       (table (:table render-metadata) items onclick-handler-fn))
     (cmp/hidden
       {:only           ["lg" "xl"]
        :implementation "js"}
       (list (:list render-metadata) items onclick-handler-fn))]))
