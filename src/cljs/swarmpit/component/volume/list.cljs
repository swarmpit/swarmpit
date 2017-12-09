(ns swarmpit.component.volume.list
  (:require [material.component :as comp]
            [material.component.panel :as panel]
            [material.component.list-table :as list]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(def headers [{:name  "Name"
               :width "50%"}
              {:name  "Driver"
               :width "50%"}])

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

(defn- volumes-handler
  []
  (handler/get
    (routes/path-for-backend :volumes)
    {:on-success (fn [response]
                   (state/update-value [:items] response cursor))}))

(defn- init-state
  []
  (state/set-value {:filter {:volumeName ""}} cursor))

(def mixin-init-state
  (mixin/init-state
    (fn []
      (init-state)
      (volumes-handler))))

(rum/defc form < rum/reactive
                 mixin-init-state
                 mixin/focus-filter []
  (let [{:keys [filter items]} (state/react cursor)
        filtered-items (filter-items items (:volumeName filter))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/text-field
         {:id       "filter"
          :hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value [:filter :volumeName] v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :volume-create)
            :label   "New volume"
            :primary true}))]]
     (list/table headers
                 (sort-by :volumeName filtered-items)
                 (nil? items)
                 render-item
                 render-item-keys
                 onclick-handler)]))