(ns material.component.list.basic
  (:refer-clojure :exclude [list])
  (:require [material.components :as cmp]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(defn status
  [label]
  (html [:div.Swarmpit-table-status label]))

(defn table-head
  [render-metadata]
  (cmp/table-head
    {:key "table-head"}
    (cmp/table-row
      {:key "table-head-row"}
      (map-indexed
        (fn [index header]
          (cmp/table-cell
            {:key       (str "table-head-cell-" index)
             :className "Swarmpit-table-head-cell"}
            (:name header))) (:summary render-metadata)))))

(defn table-body
  [render-metadata items onclick-handler-fn]
  (cmp/table-body
    {:key "table-body"}
    (map-indexed
      (fn [index item]
        (cmp/table-row
          {:key     (str "table-row-" index)
           :onClick #(onclick-handler-fn item)
           :hover   true}
          (->> (:summary render-metadata)
               (map-indexed
                 (fn [coll-index coll]
                   (let [render-fn (:render-fn coll)]
                     (cmp/table-cell
                       (merge
                         {:key       (str "table-row-cell-" index "-" coll-index)
                          :className "Swarmpit-table-row-cell"}
                         (when onclick-handler-fn
                           {:style {:cursor "pointer"}}))
                       (render-fn item index)))))))) items)))

(rum/defc table < rum/static
  [render-metadata items onclick-handler-fn]
  (cmp/table
    {:key       "rtable"
     :className "Swarmpit-table"}
    (table-head render-metadata)
    (table-body render-metadata items onclick-handler-fn)))

(defn list-item
  [render-metadata index item last onclick-handler-fn]
  (let [status-fn (:status-fn render-metadata)
        primary-key (:primary render-metadata)
        secodary-key (:secondary render-metadata)]
    (cmp/list-item
      {:key     (str "list-item-" index)
       :button  (some? onclick-handler-fn)
       :divider (false? (= item last))
       :onClick #(onclick-handler-fn item)}
      (cmp/list-item-text
        (merge
          {:key       (str "list-item-text-" index)
           :className "Swarmpit-list-item-text"
           :classes   {:primary   "Swarmpit-list-item-text-primary"
                       :secondary "Swarmpit-list-item-text-secondary"}
           :primary   (primary-key item)}
          (when secodary-key
            {:secondary (secodary-key item)})))
      (when status-fn
        (cmp/list-item-secondary-action
          {:key   (str "list-status-" index)
           :style {:marginRight "10px"}}
          (status-fn item))))))

(rum/defc list < rum/static [render-metadata items onclick-handler-fn]
  (cmp/list
    {:key   "rlist"
     :dense true}
    (map-indexed
      (fn [index item]
        (list-item
          render-metadata
          index
          item
          (last items)
          onclick-handler-fn)) items)))

(rum/defc responsive < rum/static
  [render-metadata items onclick-handler-fn]
  (cmp/mui
    (html
      [:div
       (cmp/hidden
         {:only           ["xs" "sm" "md"]
          :implementation "js"}
         (rum/with-key
           (table (:table render-metadata) items onclick-handler-fn)
           "rtable-wrapper"))
       (cmp/hidden
         {:only           ["lg" "xl"]
          :implementation "js"}
         (rum/with-key
           (list (:list render-metadata) items onclick-handler-fn)
           "rlist-wrapper"))])))
