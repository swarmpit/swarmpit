(ns swarmpit.component.dockerhub.list
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(def cursor [:page :dockerhub :list :filter])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? % predicate) items))

(rum/defc dockeruser-list < rum/reactive [items]
  (let [{:keys [name]} (state/react cursor)
        filtered-items (filter-items items name)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by user"
          :onChange (fn [_ v]
                      (state/update-value [:name] v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :dockerhub-create)
            :label   "Add User"
            :primary true}))]]
     (comp/single-list filtered-items
                       (fn [item] (print item)))]))

(defn- init-state
  []
  (state/set-value {:name ""} cursor))

(defn mount!
  [items]
  (init-state)
  (rum/mount (dockeruser-list items) (.getElementById js/document "content")))