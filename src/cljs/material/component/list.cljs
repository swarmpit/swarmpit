(ns material.component.list
  (:require [material.component :as cmp]
            [material.icon :as icon]
            [material.component.form :as form]
            [swarmpit.utils :refer [select-keys* map-values]]
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
  [render-metadata items onclick-handler-fn]
  (let [])
  (cmp/table-body
    {:key "Swarmpit-data-table-body"}
    (map-indexed
      (fn [index item]
        (cmp/table-row
          {:key     (str "Swarmpit-data-table-body-row-" index)
           :onClick #(onclick-handler-fn item)
           :hover   true}
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
  [render-metadata items onclick-handler-fn]
  (cmp/paper
    {}
    (cmp/table
      {:key       "Swarmpit-data-table"
       :className "Swarmpit-data-table"}
      (data-table-head render-metadata)
      (data-table-body render-metadata items onclick-handler-fn))))

(defn data-list-expandable-item-lines
  [render-metadata item]
  (let [labels (map :name render-metadata)]
    (form/envelope
      (->> (select-keys* item (render-keys render-metadata))
           (map-indexed
             (fn [coll-index coll]
               (let [render-fn (:render-fn (nth render-metadata coll-index))
                     value (val coll)
                     name (nth labels coll-index)]
                 (when (or (and (coll? value)
                                (not (empty? value)))
                           (some? value))
                   (if render-fn
                     (form/item name (render-fn value item))
                     (form/item name value))))))))))

(defn data-list-expandable-item
  [render-metadata index item summary-status onclick-handler-fn]
  (let [expanded (rum/react active-panel)
        summary (get-in item (primary-key render-metadata))]
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
          [:div.Swarmpit-data-list-expandable-panel-summary-content summary-status
           [:div
            (cmp/typography
              {:key          (str "Swarmpit-data-list-expandable-panel-summary-text-" index)
               :className    "Swarmpit-data-list-expandable-panel-summary-text"
               :gutterBottom true
               :noWrap       true
               :variant      "subheading"} summary)]]))
      (cmp/expansion-panel-details
        {:key (str "Swarmpit-data-list-expandable-panel-details-" index)}
        (data-list-expandable-item-lines render-metadata item))
      (cmp/divider)
      (cmp/expansion-panel-actions
        {}
        (cmp/button {:size    "small"
                     :onClick #(onclick-handler-fn item)
                     :color   "primary"} "Details")))))

(defn data-list
  [render-metadata render-status-fn items onclick-handler-fn]
  (html
    [:div.Swarmpit-data-list
     (map-indexed
       (fn [index item]
         (data-list-expandable-item
           render-metadata
           index
           item
           (when render-status-fn (render-status-fn item))
           onclick-handler-fn)) items)]))

(rum/defc responsive-table < rum/reactive
  [render-metadata render-status-fn items onclick-handler-fn]
  (html
    [:div
     (cmp/hidden
       {:only           ["xs" "sm" "md"]
        :implementation "js"}
       (data-table render-metadata items onclick-handler-fn))
     (cmp/hidden
       {:only           ["lg" "xl"]
        :implementation "js"}
       (data-list render-metadata render-status-fn items onclick-handler-fn))]))
