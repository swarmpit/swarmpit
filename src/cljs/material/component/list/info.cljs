(ns material.component.list.info
  (:require [material.icon :as icon]
            [material.component :as cmp]
            [material.component.list.util :refer [primary-key render-keys render-value?]]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.utils :refer [select-keys*]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(def active-panel (atom nil))

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

(defn list-item-detail
  [name value]
  (cmp/grid
    {:container true
     :className "Swarmpit-form-item"}
    (cmp/grid
      {:item      true
       :xs        6
       :className "Swarmpit-form-item-label"} name)
    (cmp/grid
      {:item true
       :xs   6} value)))

(defn list-item-details
  [render-metadata item]
  (let [labels (map :name render-metadata)]
    (cmp/grid
      {:container true
       :direction "column"
       :xs        12
       :sm        6}
      (->> (select-keys* item (render-keys render-metadata))
           (map-indexed
             (fn [coll-index coll]
               (let [value (val coll)
                     name (nth labels coll-index)]
                 (when (render-value? value)
                   (list-item-detail name value)))))))))

(defn list-item
  [render-metadata index item onclick-handler-fn]
  (let [expanded (rum/react active-panel)
        summary (get-in item (primary-key render-metadata))]
    (cmp/expansion-panel
      {:key      (str "Swarmpit-list-expansion-panel-" index)
       :expanded (= expanded index)
       :onChange #(if (= expanded index)
                    (reset! active-panel false)
                    (reset! active-panel index))}
      (cmp/expansion-panel-summary
        {:key        (str "Swarmpit-list-expansion-panel-summary-" index)
         :className  "Swarmpit-list-expansion-panel-summary"
         :expandIcon icon/expand-more}
        (cmp/typography
          {:key          (str "Swarmpit-list-expansion-panel-summary-text-" index)
           :className    "Swarmpit-list-expansion-panel-summary-text"
           :gutterBottom true
           :noWrap       false
           :variant      "subheading"} summary))
      (cmp/expansion-panel-details
        {:key (str "Swarmpit-list-expansion-panel-details-" index)}
        (list-item-details render-metadata item))
      (when onclick-handler-fn
        (html
          [:div
           (cmp/divider)
           (cmp/expansion-panel-actions
             {}
             (cmp/button {:size    "small"
                          :onClick #(onclick-handler-fn item)
                          :color   "primary"} "Details"))])))))

(defn list
  [render-metadata items onclick-handler-fn]
  (html
    [:div.Swarmpit-list
     (map-indexed
       (fn [index item]
         (list-item
           render-metadata
           index
           item
           onclick-handler-fn)) items)]))

(rum/defc responsive < rum/reactive
  [render-metadata items onclick-handler-fn]
  (html
    [:div
     (cmp/hidden
       {:only           ["xs" "sm" "md"]
        :implementation "js"}
       (table render-metadata items onclick-handler-fn))
     (cmp/hidden
       {:only           ["lg" "xl"]
        :implementation "js"}
       (list render-metadata items onclick-handler-fn))]))
