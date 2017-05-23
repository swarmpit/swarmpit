(ns swarmpit.component.network.list
  (:require [material.component :as comp]
            [swarmpit.uri :refer [dispatch!]]
            [swarmpit.component.state :as state]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :network :list])

(def headers ["Name" "Driver" "Internal"])

(def filter-style
  {:height     "44px"
   :lineHeight "15px"})

(def filter-underline-style
  {:borderColor "rgba(0, 0, 0, 0.2)"})

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:name %) predicate) items))

(defn- render-item
  [item]
  (case (val item)
    true "yes"
    false "no"
    (val item)))

(rum/defc network-list < rum/reactive [items]
  (let [{:keys [predicate]} (state/react cursor)
        filtered-items (filter-items items predicate)
        network-id (fn [index] (:id (nth filtered-items index)))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/mui
         (comp/text-field
           {:hintText       "Filter by name"
            :onChange       (fn [e v]
                              (state/update-value :predicate v cursor))
            :underlineStyle filter-underline-style
            :style          filter-style}))]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    "/#/networks/create"
            :label   "Create"
            :primary true}))]]
     (comp/mui
       (comp/table
         {:selectable  false
          :onCellClick (fn [i] (dispatch!
                                 (str "/#/networks/" (network-id i))))}
         (comp/list-table-header headers)
         (comp/list-table-body filtered-items
                               render-item
                               [:name :driver :internal])))]))

(defn mount!
  [items]
  (rum/mount (network-list items) (.getElementById js/document "content")))