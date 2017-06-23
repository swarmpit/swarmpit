(ns swarmpit.component.volume.list
  (:require [material.component :as comp]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :volume :list])

(def headers ["Name" "Driver"])

(def render-item-keys
  [[:volumeName] [:driver]])

(defn- render-item
  [item _]
  (val item))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :volume-info {:name (:volumeName item)}))

(defn- filter-items
  [items predicate]
  (filter #(string/includes? (:volumeName %) predicate) items))

(defn- init-state
  []
  (state/set-value {:filter {:volumeName ""}} cursor))

(def init-state-mixin
  (mixin/init
    (fn [_]
      (init-state))))

(rum/defc form < rum/reactive
                 init-state-mixin [items]
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
            :label   "New volume"
            :primary true}))]]
     (comp/list-table headers
                      (sort-by :volumeName filtered-items)
                      render-item
                      render-item-keys
                      onclick-handler)]))

