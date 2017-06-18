(ns swarmpit.component.secret.list
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :secret :list])

(def headers ["Name" "Created"])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:secretName %) predicate) items))

(def render-item-keys
  [[:secretName] [:createdAt]])

(defn- render-item
  [item _]
  (val item))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :secret-info (select-keys item [:id])))

(rum/defc secret-list < rum/reactive [items]
  (let [{{:keys [secretName]} :filter} (state/react cursor)
        filtered-items (filter-items items secretName)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value [:filter :secretName] v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :secret-create)
            :label   "Create"
            :primary true}))]]
     (comp/list-table headers
                      (sort-by :secretName filtered-items)
                      render-item
                      render-item-keys
                      onclick-handler)]))

(defn- init-state
  []
  (state/set-value {:filter {:secretName ""}} cursor))

(defn mount!
  [items]
  (init-state)
  (rum/mount (secret-list items) (.getElementById js/document "content")))