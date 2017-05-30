(ns swarmpit.component.repository.list
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [clojure.string :as string]
            [rum.core :as rum]))

(def cursor [:page :repository :list :filter])

(def headers ["Name" "Registry" "Registry Url"])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:name %) predicate) items))

(def render-item-keys
  [[:name] [:registry] [:registryUrl]])

(defn- render-item
  [item]
  (let [value (val item)]
    value))

(rum/defc repository-list < rum/reactive [items]
  (let [{:keys [name]} (state/react cursor)
        filtered-items (filter-items items name)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value :name v cursor))})
       [:span.form-panel-space]
       (comp/panel-select-field
         ""
         {:value "host"}
         (comp/menu-item
           {:key         "fdi1"
            :value       "overlay"
            :primaryText "overlay"})
         (comp/menu-item
           {:key         "fdi2"
            :value       "host"
            :primaryText "host"}))]]
     (comp/list-table headers
                      filtered-items
                      render-item
                      render-item-keys
                      "/#/repositories/")]))

(defn- init-state
  []
  (state/set-value {:name ""} cursor))

(defn mount!
  [items]
  (init-state)
  (rum/mount (repository-list items) (.getElementById js/document "content")))