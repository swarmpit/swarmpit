(ns material.component.responsive-table
  (:require [material.component :as cmp]
            [material.icon :as icon]
            [material.component.form :as form]
            [swarmpit.utils :refer [select-keys*]]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(def active-panel (atom nil))

(defn- primary-key
  [render-metadata]
  (->> (filter #(= true (:primary %)) render-metadata)
       (first)
       :key))

(defn- render-keys
  [render-metadata]
  (->> render-metadata
       (map :key)
       (into [])))

(defn data-table-head
  [render-metadata]
  (cmp/table-head
    {:key "Swarmpit-data-table-head"}
    (cmp/table-row
      {:key "Swarmpit-data-table-head-row"}
      (map-indexed
        (fn [index header]
          (cmp/table-cell
            {:key       (str "Swarmpit-data-table-head-cell-" index)
             :className "Swarmpit-data-table-head-cell"}
            (:name header))) render-metadata))))

(defn data-table-body
  [render-metadata items]
  (let [])
  (cmp/table-body
    {:key "Swarmpit-data-table-body"}
    (map-indexed
      (fn [index item]
        (cmp/table-row
          {:key   (str "Swarmpit-data-table-body-row-" index)
           :hover true}
          (->> (select-keys* item (render-keys render-metadata))
               (map-indexed
                 (fn [coll-index coll]
                   (cmp/table-cell
                     {:key (str "Swarmpit-data-table-row-cell-" index "-" coll-index)}
                     (let [render-fn (:render-fn (nth render-metadata coll-index))
                           value (val coll)]
                       (if render-fn
                         (render-fn value item)
                         value)))))))) items)))

(defn data-table
  [render-metadata items]
  (let [item (fn [index] (nth items index))]
    (cmp/paper
      {}
      (cmp/table
        {:key       "Swarmpit-data-table"
         :className "Swarmpit-data-table"}
        (data-table-head render-metadata)
        (data-table-body render-metadata items)))))

(defn expandable-data-list-item
  [index summary status details]
  (let [expanded (rum/react active-panel)]
    (cmp/expansion-panel
      {:key      (str "Swarmpit-data-list-expandable-panel-" index)
       :expanded (= expanded summary)
       :onChange #(if (= expanded summary)
                    (reset! active-panel false)
                    (reset! active-panel summary))}
      (cmp/expansion-panel-summary
        {:key        (str "Swarmpit-data-list-expandable-panel-summary-" index)
         :className  "Swarmpit-data-list-expandable-panel-summary"
         :expandIcon icon/expand-more}
        (html
          [:div.Swarmpit-data-list-expandable-panel-summary-content
           (when status
             [:div.Swarmpit-icon-ok icon/check-circle])
           [:div
            (cmp/typography
              {:key          (str "Swarmpit-data-list-expandable-panel-summary-text-" index)
               :className    "Swarmpit-data-list-expandable-panel-summary-text"
               :gutterBottom true
               :variant      "subheading"} summary)]]))
      (cmp/expansion-panel-details
        {:key (str "Swarmpit-data-list-expandable-panel-details-" index)}
        (form/envelope details))
      (cmp/divider)
      (cmp/expansion-panel-actions
        {}
        (cmp/button {:size  "small"
                     :color "primary"} "Details")))))

(defn data-list
  [render-metadata status-key items]
  (let [pk (primary-key render-metadata)
        labels (map :name render-metadata)]
    (map-indexed
      (fn [index item]
        (expandable-data-list-item
          index
          (get-in item pk)
          (get-in item status-key)
          (->> (select-keys* item (render-keys render-metadata))
               (map-indexed
                 (fn [coll-index coll]
                   (let [render-fn (:render-fn (nth render-metadata coll-index))
                         value (val coll)
                         name (nth labels coll-index)]
                     (when (not (empty? value))
                       (if render-fn
                         (form/item name (render-fn value item))
                         (form/item name value))))))))) items)))

(rum/defc responsive-table < rum/reactive
  [render-metadata status-key items]
  (html
    [:div
     (cmp/hidden
       {:only           ["xs" "sm" "md"]
        :implementation "js"}
       (data-table render-metadata items))
     (cmp/hidden
       {:only           ["lg" "xl"]
        :implementation "js"}
       (data-list render-metadata status-key items))]))
