(ns swarmpit.component.registry.list
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [clojure.string :as string]
            [rum.core :as rum]))

(def cursor [:page :registry :list :filter])

(def headers ["Name" "Version" "Url" "Is Private"])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:name %) predicate) items))

(def render-item-keys
  [[:name] [:version] [:url] [:isPrivate]])

(defn- render-item
  [item]
  (let [value (val item)]
    (case (key item)
      :isPrivate (comp/checkbox {:checked value})
      value)))

(rum/defc registry-list < rum/reactive [items]
  (let [{:keys [name]} (state/react cursor)
        filtered-items (filter-items items name)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value [:name] v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    "/#/registries/create"
            :label   "Create"
            :primary true}))]]
     (comp/list-table headers
                      filtered-items
                      render-item
                      render-item-keys
                      "/#/registries/")]))

(defn- init-state
  []
  (state/set-value {:name ""} cursor))

(defn mount!
  [items]
  (init-state)
  (rum/mount (registry-list items) (.getElementById js/document "content")))