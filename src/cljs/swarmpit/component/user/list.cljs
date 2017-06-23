(ns swarmpit.component.user.list
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(def cursor [:page :user :list])

(def headers ["Username" "Email" "Is Admin"])

(def render-item-keys
  [[:username] [:email] [:role]])

(defn- render-item
  [item _]
  (let [value (val item)]
    (case (key item)
      :role (if (= "admin" value)
              (comp/svg icon/ok)
              "")
      value)))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :user-info {:id (:_id item)}))

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
           {:href    (routes/path-for-frontend :user-create)
            :label   "New user"
            :primary true}))]]
     (comp/list-table headers
                      (sort-by :username filtered-items)
                      render-item
                      render-item-keys
                      onclick-handler)]))

