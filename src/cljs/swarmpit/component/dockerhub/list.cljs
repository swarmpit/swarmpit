(ns swarmpit.component.dockerhub.list
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(def cursor [:page :dockerhub :list :filter])

(def headers ["Name" "Username" "Company"])

(def render-item-keys
  [[:name] [:username] [:company]])

(defn- render-item
  [item]
  (val item))

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:name %) predicate) items))

(rum/defc dockeruser-list < rum/reactive [items]
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
           {:href    (routes/path-for-frontend :dockerhub-user-create)
            :label   "Add User"
            :primary true}))]]
     (comp/list-table headers
                      filtered-items
                      render-item
                      render-item-keys
                      (fn [i] (routes/path-for-frontend :dockerhub-user-info {:id (:_id i)})))]))

(defn- init-state
  []
  (state/set-value {:name ""} cursor))

(defn mount!
  [items]
  (init-state)
  (rum/mount (dockeruser-list items) (.getElementById js/document "content")))