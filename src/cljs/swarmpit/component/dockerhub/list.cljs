(ns swarmpit.component.dockerhub.list
  (:require [material.component :as comp]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [swarmpit.storage :as storage]
            [rum.core :as rum]))

(def cursor [:page :dockerhub :list])

(def headers [{:name  "Username"
               :width "30%"}
              {:name  "Name"
               :width "30%"}
              {:name  "Company"
               :width "30%"}
              {:name  "Public"
               :width "10%"}])

(def render-item-keys
  [[:username] [:name] [:company] [:public]])

(defn- render-item
  [item _]
  (let [value (val item)]
    (case (key item)
      :public (if value
                "yes"
                "no")
      value)))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :dockerhub-user-info {:id (:_id item)}))

(defn- filter-items
  [items predicate]
  (filter #(and (string/includes? (:username %) predicate)
                (= (:owner %)
                   (storage/user))) items))

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