(ns swarmpit.component.node.list
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.util :as list-util]
            [material.component.form :as form]
            [material.component.label :as label]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [goog.string.format]
            [clojure.contrib.inflect :as inflect]
            [clojure.contrib.humanize :as humanize]))

(enable-console-print!)

(defn- node-item-state [value]
  (case value
    "ready" (label/green value)
    "down" (label/red value)))

(defn- node-item-labels [item]
  (form/item-labels
    [(node-item-state (:state item))
     (when (:leader item)
       (label/primary "leader"))
     (label/grey (:role item))
     (if (= "active" (:availability item))
       (label/green "active")
       (label/grey (:availability item)))]))

(defn node-used [stat]
  (cond
    (< stat 75) {:name  "actual"
                 :value stat
                 :color "#43a047"}
    (> stat 90) {:name  "actual"
                 :value stat
                 :color "#d32f2f"}
    :else {:name  "actual"
           :value stat
           :color "#ffa000"}))

(rum/defc node-graph [stat label]
  (let [data [(node-used stat)
              {:name  "rest"
               :value (- 100 stat)
               :color "#ccc"}]]
    (html
      [:div {:style {:width  "120px"
                     :height "120px"}}
       (comp/responsive-container
         (comp/pie-chart
           {}
           (comp/pie
             {:data        data
              :cx          "50"
              :innerRadius "60%"
              :outerRadius "80%"
              :startAngle  90
              :endAngle    -270
              :fill        "#8884d8"}
             (map #(comp/cell {:fill (:color %)}) data)
             (comp/re-label
               {:width    30
                :position "center"} label))))])))

(defn- node-item
  [item]
  (let [cpu (-> item :resources :cpu (int))
        memory-bytes (-> item :resources :memory (* 1024 1024))
        disk-bytes (-> item :stats :disk :total)]
    (comp/grid
      {:item true}
      (html
        [:a {:href  (routes/path-for-frontend :node-info {:id (:id item)})
             :style {:color          "inherit"
                     :textDecoration "inherit"}}
         (comp/card
           {:className "Swarmpit-form-card"}
           (comp/card-header
             {:title     (:nodeName item)
              :className "Swarmpit-form-card-header"
              :subheader (:address item)
              :avatar    (comp/svg (icon/os (:os item)))})
           (comp/card-content
             {}
             (str "docker " (:engine item)))
           (comp/card-content
             {}
             (node-item-labels item))
           (comp/card-content
             {:className "Swarmpit-table-card-content"}
             (html
               [:div.Swarmpit-node-stat
                (node-graph
                  (get-in item [:stats :cpu :usedPercentage])
                  (str cpu " " (inflect/pluralize-noun cpu "core")))
                (node-graph
                  (get-in item [:stats :disk :usedPercentage])
                  (str (humanize/filesize disk-bytes :binary false) " disk"))
                (node-graph
                  (get-in item [:stats :memory :usedPercentage])
                  (str (humanize/filesize memory-bytes :binary false) " ram"))])))]))))

(defn- nodes-handler
  []
  (ajax/get
    (routes/path-for-backend :nodes)
    {:on-success (fn [{:keys [response]}]
                   (state/update-value [:items] response state/form-value-cursor))}))

(defn form-search-fn
  [event]
  (state/update-value [:filter :query] (-> event .-target .-value) state/form-state-cursor))

(defn- init-form-state
  []
  (state/set-value {:filter {:query ""}} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (nodes-handler))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [filter]} (state/react state/form-state-cursor)
        filtered-items (list-util/filter items (:query filter))]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (comp/grid
            {:container true
             :spacing   40}
            (->> (sort-by :nodeName filtered-items)
                 ;(map #(rum/with-key (node-item %) (:id %)))
                 (map #(node-item %))))]]))))

