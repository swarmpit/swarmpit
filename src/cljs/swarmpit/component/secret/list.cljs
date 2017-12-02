(ns swarmpit.component.secret.list
  (:require [material.component :as comp]
            [material.component.panel :as panel]
            [material.component.list-table :as list]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [swarmpit.time :as time]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:page :secret :list])

(def headers [{:name  "Name"
               :width "50%"}
              {:name  "Created"
               :width "50%"}])

(def render-item-keys
  [[:secretName] [:createdAt]])

(defn- render-item
  [item _]
  (val item))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :secret-info (select-keys item [:id])))

(defn- filter-items
  [items predicate]
  (filter #(string/includes? (:secretName %) predicate) items))

(defn- init-state
  []
  (state/set-value {:filter {:secretName ""}} cursor))

(def init-state-mixin
  (mixin/init
    (fn [_]
      (init-state))))

(rum/defc form < rum/reactive
                 init-state-mixin
                 mixin/focus-filter [items]
  (let [{{:keys [secretName]} :filter} (state/react cursor)
        filtered-items (filter-items items secretName)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/text-field
         {:id       "filter"
          :hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value [:filter :secretName] v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :secret-create)
            :label   "New secret"
            :primary true}))]]
     (list/table headers
                 (->> filtered-items
                           (sort-by :secretName)
                           (map #(update % :createdAt time/simplify)))
                 render-item
                 render-item-keys
                 onclick-handler)]))