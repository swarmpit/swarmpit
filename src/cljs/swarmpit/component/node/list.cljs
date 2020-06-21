(ns swarmpit.component.node.list
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.util :as list-util]
            [material.component.form :as form]
            [material.component.label :as label]
            [material.component.chart :as chart]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.common :as common]
            [swarmpit.component.progress :as progress]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [goog.string.format]
            [goog.string :as gstring]
            [clojure.contrib.inflect :as inflect]
            [clojure.contrib.humanize :as humanize]))

(enable-console-print!)

(defn- node-item-state [value]
  (case value
    "ready" (label/base value "green")
    "down" (label/base value "red")))

(defn- node-item-labels [item]
  (form/item-labels
    [(node-item-state (:state item))
     (when (:leader item)
       (label/base "leader" "primary"))
     (label/base (:role item) "grey")
     (if (= "active" (:availability item))
       (label/base "active" "green")
       (label/base (:availability item) "grey"))]))

(rum/defc node-stats < rum/static [item index]
  (let [cpu-usage (get-in item [:stats :cpu :usedPercentage])
        cpu-limit (get-in item [:stats :cpu :cores])
        disk (get-in item [:stats :disk :used])
        disk-usage (get-in item [:stats :disk :usedPercentage])
        disk-limit (get-in item [:stats :disk :total])
        memory (get-in item [:stats :memory :used])
        memory-usage (get-in item [:stats :memory :usedPercentage])
        memory-limit (get-in item [:stats :memory :total])]
    (if (= "down" (:state item))
      (comp/box
        {:className "Swarmpit-stat-empty"})
      (comp/box
        {:class "Swarmpit-stat"
         :key   (str "node-card-stat-" index)}
        (common/resource-pie
          {:value (* cpu-limit (/ cpu-usage 100))
           :limit cpu-limit
           :usage cpu-usage
           :type  :cpu}
          (str cpu-limit " vCPU")
          (str "graph-cpu-" index))
        (common/resource-pie
          {:value disk
           :limit disk-limit
           :usage disk-usage
           :type  :disk}
          (str (common/render-capacity disk-limit false) " disk")
          (str "graph-disk-" index))
        (common/resource-pie
          {:value memory
           :limit memory-limit
           :usage memory-usage
           :type  :memory}
          (str (common/render-capacity memory-limit true) " ram")
          (str "graph-memory-" index))))))

(rum/defc node-item < rum/static [item index]
  (comp/grid
    {:item true
     :xs   12
     :sm   6
     :md   6
     :lg   4
     :xl   3
     :key  (str "node-" index)}
    (html
      [:a {:class "Swarmpit-node-href"
           :key   (str "node-href--" index)
           :href  (routes/path-for-frontend :node-info {:id (:id item)})}
       (comp/card
         {:className "Swarmpit-form-card"
          :key       (str "node-card-" index)}
         (comp/card-header
           {:title     (:nodeName item)
            :className "Swarmpit-form-card-header"
            :key       (str "node-card-header-" index)
            :subheader (:address item)
            :avatar    (comp/svg (icon/os-path (:os item)))})
         (comp/card-content
           {:key (str "node-card-engine-" index)}
           (str "docker " (:engine item)))
         (comp/card-content
           {:key (str "node-card-labels-" index)}
           (node-item-labels item))
         (comp/card-content
           {:className "Swarmpit-table-card-content"
            :key       (str "node-card-stats-" index)}
           (node-stats item index)))])))

(defn- nodes-handler
  []
  (ajax/get
    (routes/path-for-backend :nodes)
    {:state      [:loading?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (state/update-value [:items] response state/form-value-cursor)))}))

(defn form-search-fn
  [event]
  (state/update-value [:query] (-> event .-target .-value) state/search-cursor))

(defn- init-form-state
  []
  (state/set-value {:loading?    false
                    :filter      {:role nil}
                    :filterOpen? false} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (nodes-handler))))

(defn pinned
  [nodes]
  (comp/grid
    {:container true
     :spacing   2}
    (map-indexed
      (fn [index item]
        (node-item item index)) (sort-by :nodeName nodes))))

(rum/defc form-filters < rum/static [filterOpen? {:keys [role] :as filter}]
  (common/list-filters
    filterOpen?
    (comp/text-field
      {:fullWidth       true
       :label           "Role"
       :helperText      "Filter by node role"
       :select          true
       :value           role
       :variant         "outlined"
       :margin          "normal"
       :InputLabelProps {:shrink true}
       :onChange        #(state/update-value [:filter :role] (-> % .-target .-value) state/form-state-cursor)}
      (comp/menu-item
        {:key   "manager"
         :value "manager"} "manager")
      (comp/menu-item
        {:key   "worker"
         :value "worker"} "worker"))))

(def toolbar-render-metadata
  [{:name     "Show filters"
    :onClick  #(state/update-value [:filterOpen?] true state/form-state-cursor)
    :icon     (icon/filter-list)
    :icon-alt (icon/filter-list)
    :variant  "outlined"
    :color    "default"}])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [query]} (state/react state/search-cursor)
        {:keys [loading? filterOpen? filter]} (state/react state/form-state-cursor)
        filtered-items (->> (list-util/filter items query)
                            (clojure.core/filter #(if (some? (:role filter))
                                                    (= (:role filter) (:role %))
                                                    true)))]
    (progress/form
      loading?
      (comp/box
        {}
        (common/list-grid
          "Nodes"
          items
          filtered-items
          (comp/grid
            {:container true
             :spacing   2}
            (map-indexed
              (fn [index item]
                (node-item item index)) (sort-by :nodeName filtered-items)))
          toolbar-render-metadata)
        (form-filters filterOpen? filter)))))