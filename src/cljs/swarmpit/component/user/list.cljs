(ns swarmpit.component.user.list
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(def cursor [:page :user :list :filter])

(def headers ["Username" "Email" "Is Admin"])

(defn- filter-items
  "Filter list items based on given predicate"
  [items predicate]
  (filter #(string/includes? (:username %) predicate) items))

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

(rum/defc user-list < rum/reactive [items]
  (let [{:keys [username]} (state/react cursor)
        filtered-items (filter-items items username)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (comp/panel-text-field
         {:hintText "Filter by username"
          :onChange (fn [_ v]
                      (state/update-value [:username] v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :user-create)
            :label   "Create"
            :primary true}))]]
     (comp/list-table headers
                      (sort-by :username filtered-items)
                      render-item
                      render-item-keys
                      onclick-handler)]))

(defn- init-state
  []
  (state/set-value {:username ""} cursor))

(defn mount!
  [items]
  (init-state)
  (rum/mount (user-list items) (.getElementById js/document "content")))