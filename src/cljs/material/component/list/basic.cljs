(ns material.component.list.basic
  (:refer-clojure :exclude [list])
  (:require [material.components :as cmp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

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
            (cmp/box
              {:className "Swarmpit-row"}
              (cmp/typography {:variant "subtitle2"} (:name header))
              (when (:tooltip header)
                (cmp/tooltip
                  {:title     (:tooltip header)
                   :placement "bottom"}
                  (icon/help-outline {:className "Swarmpit-table-head-tooltip"
                                       :fontSize  "small"}))))))
        (:summary render-metadata)))))

(defn table-body
  [render-metadata items onclick-handler-fn]
  (cmp/table-body
    {:key "table-body"}
    (map-indexed
      (fn [index item]
        (let [route (when onclick-handler-fn (onclick-handler-fn item))]
          (cmp/table-row
            (merge
              {:key       (str "table-row-" index)
               :className "Swarmpit-table-row"
               :hover     true}
              (when route
                {:onClick #(dispatch! route)}))
            (->> (:summary render-metadata)
                 (map-indexed
                   (fn [coll-index coll]
                     (let [render-fn (:render-fn coll)]
                       (cmp/table-cell
                         (merge
                           {:key       (str "table-row-cell-" index "-" coll-index)
                            :className "Swarmpit-table-row-cell"}
                           (when route
                             {:style {:cursor "pointer"}})
                           (when (:status coll)
                             {:style {:textAlign "right"}}))
                         (if (zero? coll-index)
                           (html
                             [:a {:href  route
                                  :style {:display "block"}}
                              (render-fn item index)])
                           (render-fn item index)))))))))) items)))

(rum/defc table < rum/static
  [render-metadata items onclick-handler-fn]
  (cmp/table
    {:className "Swarmpit-table"}
    (table-head render-metadata)
    (table-body render-metadata items onclick-handler-fn)))

(defn list-item
  [render-metadata index item last onclick-handler-fn]
  (let [status-fn (:status-fn render-metadata)
        primary-key (:primary render-metadata)
        secodary-key (:secondary render-metadata)
        route (when onclick-handler-fn (onclick-handler-fn item))]
    (cmp/list-item
      (merge
        {:key     (str "list-item-" index)
         :divider (false? (= item last))}
        (when route
          {:component "a"
           :button    true
           :href      route
           :onClick   #(dispatch! route)}))
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

(defn table-item-name [primary secondary]
  (html
    [:div
     [:div
      [:span.Swarmpit-table-cell-primary primary]]
     [:div
      [:span.Swarmpit-table-cell-secondary secondary]]]))

(defn override-title
  ([render-metadata primary-render-fn]
   (let [table-summary (-> (get-in render-metadata [:table :summary])
                           (assoc-in [0 :render-fn] #(primary-render-fn %)))]
     (-> render-metadata
         (assoc-in [:table :summary] table-summary)
         (assoc-in [:list :primary] primary-render-fn))))
  ([render-metadata primary-render-fn secondary-render-fn]
   (let [table-summary (-> (get-in render-metadata [:table :summary])
                           (assoc-in [0 :render-fn] #(table-item-name
                                                       (primary-render-fn %)
                                                       (secondary-render-fn %))))]
     (-> render-metadata
         (assoc-in [:table :summary] table-summary)
         (assoc-in [:list :primary] primary-render-fn)
         (assoc-in [:list :secondary] secondary-render-fn)))))

(defn add-status
  [render-metadata custom-render-fn]
  (let [table-summary (-> (get-in render-metadata [:table :summary])
                          (conj {:name      ""
                                 :status    true
                                 :render-fn #(custom-render-fn %)}))]
    (-> render-metadata
        (assoc-in [:table :summary] table-summary)
        (assoc-in [:list :status-fn] custom-render-fn))))

(rum/defc list < rum/static [render-metadata items onclick-handler-fn]
  (cmp/list
    {:dense          true
     :disablePadding true}
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
         {:only           ["xs" "sm"]
          :implementation "js"}
         (table (:table render-metadata) items onclick-handler-fn))
       (cmp/hidden
         {:only           ["md" "lg" "xl"]
          :implementation "js"}
         (list (:list render-metadata) items onclick-handler-fn))])))
