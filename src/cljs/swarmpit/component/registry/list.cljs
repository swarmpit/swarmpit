(ns swarmpit.component.registry.list
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(def cursor [:page :registry :list])

(def headers ["Name" "Url" "Secure"])

(def render-item-keys
  [[:name] [:url] [:withAuth]])

(defn- render-item
  [item _]
  (let [value (val item)]
    (case (key item)
      :withAuth (if value
                  (comp/svg icon/ok)
                  "")
      value)))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :registry-info {:id (:_id item)}))

(defn- filter-items
  [items predicate]
  (filter #(string/includes? (:name %) predicate) items))

(defn- init-state
  []
  (state/set-value {:filter {:name ""}} cursor))

(def init-state-mixin
  (mixin/init
    (fn [_]
      (init-state))))

(rum/defc form < rum/reactive
                 init-state-mixin [items]
  (let [{{:keys [name]} :filter} (state/react cursor)
        filtered-items (filter-items items name)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value [:filter :name] v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :registry-create)
            :label   "New registry"
            :primary true}))]]
     (comp/list-table headers
                      (sort-by :name filtered-items)
                      render-item
                      render-item-keys
                      onclick-handler)]))