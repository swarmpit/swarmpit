(ns swarmpit.component.volume.list
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :volume :list])

(def headers ["Name" "Driver"])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:volumeName %) predicate) items))

(def render-item-keys
  [[:volumeName] [:driver]])

(defn- render-item
  [item _]
  (val item))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :volume-info {:name (:volumeName item)}))

(rum/defc volume-list < rum/reactive [items]
  (let [{{:keys [volumeName]} :filter} (state/react cursor)
        filtered-items (filter-items items volumeName)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value [:filter :volumeName] v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :volume-create)
            :label   "Create"
            :primary true}))]]
     (comp/list-table headers
                      (sort-by :volumeName filtered-items)
                      render-item
                      render-item-keys
                      onclick-handler)]))

(defn- init-state
  []
  (state/set-value {:filter {:volumeName ""}} cursor))

(defn mount!
  [items]
  (init-state)
  (rum/mount (volume-list items) (.getElementById js/document "content")))