(ns swarmpit.component.node.list
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.label :as label]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [goog.string.format]
            [goog.string :as gstring]
            [clojure.contrib.humanize :as humanize]))

(enable-console-print!)

(defn- node-item-header [item]
  (comp/card-header
    {:title
     (html
       [:div.node-item-header
        [:span.node-item-header-os-icon
         [:svg.node-item-ico {:width "24"
                              :height "24"
                              :fill "rgb(117, 117, 117)"}
          [:path {:d (icon/os (:os item))}]]]
        [:span [:b (:nodeName item)]]])
     :className "Swarmpit-node-card-header"}))

(defn- node-item-state [value]
  (case value
    "ready" (label/green value)
    "down" (label/red value)))

(defn- node-item-states [item]
  (comp/card-content
    {}
    (html
      [:div.node-item-states
       [:span.node-item-state (node-item-state (:state item))]
       (when (:leader item)
         [:span.node-item-state (label/blue "leader")])
       [:span.node-item-state (label/blue (:role item))]
       [:span.node-item-state (if (= "active" (:availability item))
                                (label/blue "active")
                                (label/yellow (:availability item)))]])))

(defn- node-item-engine-and-address [item]
  (comp/card-content
    {}
    (html
      [:div.node-item-engine-and-address
       [:span.node-item-secondary "docker " (:engine item)]
       [:span.node-item-secondary (:address item)]])))

(defn- node-item-usage [stat]
  (let [stat-string (str (gstring/format "%.2f" stat) "%")]
    (cond
      (< stat 75) [:td {:style {:color "#509E50"}} stat-string]
      (> stat 90) [:td {:style {:color "rgb(244, 67, 54)"}} stat-string]
      :else [:td {:style {:color "#9e931b"}} stat-string])))

(defn- node-item-stats [item]
  (let [cpu (-> item :resources :cpu (int))
        memory-bytes (-> item :resources :memory (* 1024 1024))
        disk-bytes (-> item :stats :disk :total)]
    (comp/card-content
      {}
      (html
        [:table.node-progress-table
         {:style {:width "100%"}}
         [:tbody
          [:tr.node-progress-table-icons
           [:td
            [:img.hardware-icon {:src "img/icons-svg/cpu.svg" :alt "CPU"}]]
           [:td
            [:img.hardware-icon {:src "img/icons-svg/disk.svg" :alt "Disk"}]]
           [:td
            [:img.hardware-icon {:src "img/icons-svg/memory.svg" :alt "Memory"}]]]
          [:tr.node-progress-table-name
           [:td "CPU"]
           [:td "DISK"]
           [:td "MEMORY"]]
          [:tr.node-progress-table-values
           [:td (str cpu " " (clojure.contrib.inflect/pluralize-noun cpu "core"))]
           [:td (if (some? disk-bytes)
                  (humanize/filesize disk-bytes :binary false)
                  "-")]
           [:td (humanize/filesize memory-bytes :binary false)]]
          (when (some? (:stats item))
            [:tr.node-progress-table-usage
             (node-item-usage (get-in item [:stats :cpu :usedPercentage]))
             (node-item-usage (get-in item [:stats :disk :usedPercentage]))
             (node-item-usage (get-in item [:stats :memory :usedPercentage]))])]]))))

(defn- node-item
  [item]
  (comp/grid
    {:item true
     :xs 12
     :sm 6
     :lg 4
     :xl 3}
    (comp/card
      {:className "Swarmpit-form-card"}
      (html
        [:div.node-item
         [:a {:href (str "/#/nodes/" (:id item))
              :style {:color "inherit"
                      :textDecoration "inherit"}}
          (node-item-header item)
          (node-item-engine-and-address item)
          (node-item-states item)
          (node-item-stats item)]]))))

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
        {:keys [filter]} (state/react state/form-state-cursor)]
    ;filtered-items (list/filter items (:query filter))
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (comp/grid
            {:container true
             :spacing 40}
            (->> (sort-by :nodeName items)
                 ;(map #(rum/with-key (node-item %) (:id %)))
                 (map #(node-item %))))]]))))

