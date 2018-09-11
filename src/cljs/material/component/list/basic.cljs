(ns material.component.list.basic
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
      {:key "Swarmpit-table-head-row"}
      (map-indexed
        (fn [index header]
          (cmp/table-cell
            {:key       (str "Swarmpit-table-head-cell-" index)
             :className "Swarmpit-table-head-cell"}
            (:name header))) render-metadata))))

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
          (->> (select-keys* item (render-keys render-metadata))
               (map-indexed
                 (fn [coll-index coll]
                   (cmp/table-cell
                     {:key (str "Swarmpit-table-row-cell-" index "-" coll-index)}
                     (let [render-fn (:render-fn (nth render-metadata coll-index))
                           value (val coll)]
                       (if render-fn
                         (render-fn value item)
                         value)))))))) items)))

(defn table
  [render-metadata items onclick-handler-fn]
  (cmp/paper
    {}
    (cmp/table
      {:key       "Swarmpit-table"
       :className "Swarmpit-table"}
      (table-head render-metadata)
      (table-body render-metadata items onclick-handler-fn))))

(defn list-item-detail
  [name value]
  (cmp/grid
    {:container true
     :className "Swarmpit-form-item"}
    (cmp/grid
      {:item      true
       :xs        12
       :sm        6
       :className "Swarmpit-form-item-label"} name)
    (cmp/grid
      {:item true
       :xs   12
       :sm   6} value)))

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
               (let [render-fn (:render-fn (nth render-metadata coll-index))
                     value (val coll)
                     name (nth labels coll-index)]
                 (when (render-value? value)
                   (if render-fn
                     (list-item-detail name (render-fn value item))
                     (list-item-detail name value))))))))))

(defn list-item
  [render-metadata index item summary-status onclick-handler-fn]
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
        (html
          [:div.Swarmpit-list-expansion-panel-summary-content summary-status
           [:div
            (cmp/typography
              {:key          (str "Swarmpit-list-expansion-panel-summary-text-" index)
               :className    "Swarmpit-list-expansion-panel-summary-text"
               :gutterBottom true
               :noWrap       true
               :variant      "subheading"} summary)]]))
      (cmp/expansion-panel-details
        {:key (str "Swarmpit-list-expansion-panel-details-" index)}
        (list-item-details render-metadata item))
      (cmp/divider)
      (cmp/expansion-panel-actions
        {}
        (cmp/button {:size    "small"
                     :onClick #(onclick-handler-fn item)
                     :color   "primary"} "Details")))))

(defn list
  [render-metadata render-status-fn items onclick-handler-fn]
  (html
    [:div.Swarmpit-list
     (map-indexed
       (fn [index item]
         (list-item
           render-metadata
           index
           item
           (when render-status-fn (render-status-fn item))
           onclick-handler-fn)) items)]))

(rum/defc responsive < rum/reactive
  [render-metadata render-status-fn items onclick-handler-fn]
  (html
    [:div
     (cmp/hidden
       {:only           ["xs" "sm" "md"]
        :implementation "js"}
       (table render-metadata items onclick-handler-fn))
     (cmp/hidden
       {:only           ["lg" "xl"]
        :implementation "js"}
       (list render-metadata render-status-fn items onclick-handler-fn))]))
