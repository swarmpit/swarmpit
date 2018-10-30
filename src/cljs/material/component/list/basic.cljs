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
                       (cmp/typography
                         {:variant      "body1"
                          :color        "textSecondary"
                          :gutterBottom true}
                         (render-fn item))))))))) items)))

(defn table
  [render-metadata items onclick-handler-fn]
  (cmp/card
    {:className "Swarmpit-card"}
    (cmp/card-header
      {:className "Swarmpit-table-card-header"
       :title     (:title render-metadata)
       :subheader (:subheader render-metadata)})
    (cmp/card-content
      {:className "Swarmpit-table-card-content"}
      (cmp/table
        {:key       "Swarmpit-table"
         :className "Swarmpit-table"}
        (table-head render-metadata)
        (table-body render-metadata items onclick-handler-fn)))))

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
  [render-metadata index item onclick-handler-fn]
  (let [expanded (rum/react active-panel)
        status-fn (:status-fn render-metadata)]
    (cmp/expansion-panel
      {:key       (str "Swarmpit-list-expansion-panel-" index)
       :className "Swarmpit-list-expansion-panel"
       :expanded  (= expanded index)
       :onChange  #(if (= expanded index)
                     (reset! active-panel false)
                     (reset! active-panel index))}
      (cmp/expansion-panel-summary
        {:key        (str "Swarmpit-list-expansion-panel-summary-" index)
         :className  "Swarmpit-list-expansion-panel-summary"
         :expandIcon icon/expand-more}
        ;(when status-fn
        ;  (html
        ;    [:div.Swarmpit-list-expansion-panel-summary-content (status-fn item)]))
        (html
          [:div {:style {:flexBasis  "33.33%"
                         :flexShrink 0}}
           [:div
            (cmp/typography
              {:key       (str "Swarmpit-list-expansion-panel-summary-text-" index)
               :className "Swarmpit-list-expansion-panel-summary-text"
               :noWrap    true
               :variant   "subheading"} ((:primary-key render-metadata) item))]
           [:div
            (cmp/typography
              {:key       (str "Swarmpit-list-expansion-panel-summary-text-" index)
               :className "Swarmpit-list-expansion-panel-summary-text"
               :noWrap    true
               :variant   "caption"} ((:secondary-key render-metadata) item))]])

        (cmp/typography
          {:key       (str "Swarmpit-list-expansion-panel-summary-text-" index)
           :className "Swarmpit-list-expansion-panel-summary-text"
           :noWrap    true
           :variant   "subheading"} (status-fn item)))
      (cmp/expansion-panel-details
        {:key (str "Swarmpit-list-expansion-panel-details-" index)}
        (->> (:summary render-metadata)
             (map (fn [data]
                    (let [render-fn (:render-fn data)]
                      (render-fn item))))))
      (cmp/divider)
      (cmp/expansion-panel-actions
        {}
        (cmp/button {:size    "small"
                     :onClick #(onclick-handler-fn item)
                     :color   "primary"} "Details")))))

(defn list
  [render-metadata items onclick-handler-fn]
  (cmp/card
    {:className "Swarmpit-form-card"}
    (cmp/card-header
      {:className "Swarmpit-form-card-header"
       :title     "dsdsd"
       :subheader "Running: 3, Updating: 5"})
    (cmp/card-content
      {:className "Swarmpit-table-card-content"}
      (html
        [:div.Swarmpit-list
         (map-indexed
           (fn [index item]
             (list-item
               render-metadata
               index
               item
               onclick-handler-fn)) items)]))))

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
