(ns material.component.list.edit
  (:require [material.icon :as icon]
            [material.component :as cmp]
            [material.component.list.util :refer [primary-key render-keys]]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.utils :refer [select-keys*]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(def active-panel (atom nil))

(defn table-body
  [render-metadata items delete-handler-fn]
  (cmp/table-body
    {:key "Swarmpit-table-body"}
    (map-indexed
      (fn [index item]
        (cmp/table-row
          {:key (str "Swarmpit-table-row-" index)}
          (->> (select-keys* item (render-keys render-metadata))
               (map-indexed
                 (fn [coll-index coll]
                   (cmp/table-cell
                     {:key       (str "Swarmpit-table-row-cell-" index "-" coll-index)
                      :className "Swarmpit-table-row-cell-edit"}
                     (let [render-fn (:render-fn (nth render-metadata coll-index))
                           value (val coll)]
                       (render-fn value item))))))
          (cmp/table-cell
            {:className "Swarmpit-table-row-cell-delete"}
            (cmp/tooltip
              {:title     "Delete"
               :placement "top-start"}
              (cmp/icon-button
                {:color   "secondary"
                 :onClick #(delete-handler-fn index)}
                (cmp/svg icon/trash)))))) items)))

(defn table
  [render-metadata items delete-handler-fn]
  (cmp/table
    {:key       "Swarmpit-table"
     :className "Swarmpit-table Swarmpit-table-card Swarmpit-table-card-edit"}
    (table-body render-metadata items delete-handler-fn)))

(defn list-item-detail
  [value]
  (cmp/grid
    {:container true
     :className "Swarmpit-form-item"}
    (cmp/grid
      {:item true
       :xs   12} value)))

(defn list-item-details
  [render-metadata item index]
  (cmp/grid
    {:container true
     :direction "column"
     :xs        12
     :sm        6}
    (->> (select-keys* item (render-keys render-metadata))
         (map-indexed
           (fn [coll-index coll]
             (let [render-fn (:render-fn (nth render-metadata coll-index))
                   value (val coll)]
               (list-item-detail (render-fn value item index))))))))

(defn list-item
  [render-metadata index item delete-handler-fn]
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
           :noWrap       true
           :variant      "subheading"} summary))
      (cmp/expansion-panel-details
        {:key (str "Swarmpit-list-expansion-panel-details-" index)}
        (list-item-details render-metadata item index))
      (cmp/divider)
      (cmp/expansion-panel-actions
        {}
        (cmp/button {:size    "small"
                     :onClick #(delete-handler-fn index)
                     :color   "primary"} "Delete")))))

(defn list
  [render-metadata items delete-handler-fn]
  (html
    [:div.Swarmpit-list
     (map-indexed
       (fn [index item]
         (list-item
           render-metadata
           index
           item
           delete-handler-fn)) items)]))

(rum/defc responsive < rum/reactive
  [render-metadata items delete-handler-fn]
  (html
    [:div
     (cmp/hidden
       {:only           ["xs" "sm" "md"]
        :implementation "js"}
       (table render-metadata items delete-handler-fn))
     (cmp/hidden
       {:only           ["lg" "xl"]
        :implementation "js"}
       (list render-metadata items delete-handler-fn))]))

