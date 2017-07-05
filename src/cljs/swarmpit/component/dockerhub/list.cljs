(ns swarmpit.component.dockerhub.list
  (:require [material.component :as comp]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(def cursor [:page :dockerhub :list])

(def headers [{:name  "Username"
               :width "40%"}
              {:name  "Name"
               :width "30%"}
              {:name  "Company"
               :width "30%"}])

(def render-item-keys
  [[:username] [:name] [:company]])

(defn- render-item
  [item _]
  (val item))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :dockerhub-user-info {:id (:_id item)}))

(defn- filter-items
  [items predicate]
  (filter #(string/includes? (:username %) predicate) items))

(defn- init-state
  []
  (state/set-value {:filter {:username ""}} cursor))

(def init-state-mixin
  (mixin/init
    (fn [_]
      (init-state))))

(rum/defc form < rum/reactive
                 init-state-mixin [items]
  (let [{{:keys [username]} :filter} (state/react cursor)
        filtered-items (filter-items items username)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by username"
          :onChange (fn [_ v]
                      (state/update-value [:filter :username] v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :dockerhub-user-create)
            :label   "New user"
            :primary true}))]]
     (comp/list-table headers
                      (sort-by :username filtered-items)
                      render-item
                      render-item-keys
                      onclick-handler)]))