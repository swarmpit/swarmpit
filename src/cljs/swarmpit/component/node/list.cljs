(ns swarmpit.component.node.list
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.component.state :as state]
            [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :node :list :filter])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:nodeName %) predicate) items))

(def list-item-style
  {:minWidth   "600px"
   :marginLeft "6px"})

(def address-span-style
  {:fontWeight "lighter"
   :fontSize   "12px"
   :marginLeft "10px"})

(def span-style
  {:marginLeft "10px"})

(def span-engine-style
  {:marginLeft "50px"})

(def engine-icon-style
  {:height "12px"})

(defn form-state [value]
  (case value
    "READY" (comp/label-green value)
    "DOWN" (comp/label-red value)))

(defn- render-primary-text
  [item]
  (html [:div
         [:span (:nodeName item)]
         [:span {:style address-span-style} (:address item)]]))

(defn- render-secondary-text
  [item]
  (html [:p
         [:span (:role item)]
         [:span {:style span-style} (form-state (string/upper-case (:state item)))]
         [:span {:style span-engine-style}
          (comp/svg {:style engine-icon-style} icon/docker)]
         [:span (:engine item)]
         [:span {:style span-style} (string/upper-case (:availability item))]
         (if (:leader item)
           [:span {:style span-style} (comp/label-blue "LEADER")])]))

(rum/defc node-list < rum/reactive [items]
  (let [{:keys [nodeName]} (state/react cursor)
        filtered-items (filter-items items nodeName)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value [:nodeName] v cursor))})]]
     (comp/mui
       (comp/list
         {}
         (->> (sort-by :nodeName filtered-items)
              (map #(comp/list-item
                      {:style         list-item-style
                       :disabled      true
                       :leftIcon      (comp/svg icon/nodes)
                       :primaryText   (render-primary-text %)
                       :secondaryText (render-secondary-text %)})))))]))

(defn- init-state
  []
  (state/set-value {:nodeName ""} cursor))

(defn mount!
  [items]
  (init-state)
  (rum/mount (node-list items) (.getElementById js/document "content")))
