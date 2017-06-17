(ns swarmpit.component.node.list
  (:require [material.component :as comp]
            [material.mixin :as mixin]
            [material.icon :as icon]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [swarmpit.storage :as storage]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [clojure.walk :refer [keywordize-keys]]
            [ajax.core :as ajax]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :node :list :filter])

(def cursor-data [:page :node :list :data])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:nodeName %) predicate) items))

(defn form-state [value]
  (case value
    "ready" (comp/label-green value)
    "down" (comp/label-red value)))

(defn- data-handler
  []
  (ajax/GET (routes/path-for-backend :nodes)
            {:headers {"Authorization" (storage/get "token")}
             :handler (fn [response]
                        (keywordize-keys response)
                        (let [resp (keywordize-keys response)]
                          (state/set-value resp cursor-data)))}))

(def refresh-mixin
  (mixin/list-refresh-mixin data-handler))

(defn- node-item
  [item]
  (html
    [:div.mdl-cell.node-item
     [:div
      [:span
       [:svg.node-item-ico {::width "24" :height "24" :fill "rgb(117, 117, 117)"}
        [:path {:d icon/docker}]]]
      [:span [:b (:nodeName item)]]]
     [:div.node-item-states
      [:span.node-item-state (form-state (:state item))]
      (if (:leader item)
        [:span.node-item-state (comp/label-blue "leader")])]
     [:div
      [:span "[ " (:role item) " ]"]]
     [:div
      [:span.node-item-secondary "address: " (:address item)]]
     [:div
      [:span.node-item-secondary "engine: " (:engine item)]]
     [:div
      [:span.node-item-secondary "availability: " (:availability item)]]]))

(rum/defc node-list < rum/reactive
                      refresh-mixin []
  (let [items (state/react cursor-data)
        {:keys [nodeName]} (state/react cursor)
        filtered-items (filter-items items nodeName)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value [:nodeName] v cursor))})]]
     [:div.content-grid.mdl-grid
      (->> (sort-by :nodeName filtered-items)
           (map #(node-item %)))]]))

(defn- init-state
  [nodes]
  (state/set-value {:nodeName ""} cursor)
  (state/set-value nodes cursor-data))

(defn mount!
  [nodes]
  (init-state nodes)
  (rum/mount (node-list) (.getElementById js/document "content")))
