(ns swarmpit.component.user.list
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
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
  [item]
  (let [value (val item)]
    (case (key item)
      :role (comp/checkbox {:checked (= "admin" value)})
      value)))

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
           {:href    "/#/users/create"
            :label   "Create"
            :primary true}))]]
     (comp/list-table headers
                      filtered-items
                      render-item
                      render-item-keys
                      "/#/users/")]))

(defn- init-state
  []
  (state/set-value {:username ""} cursor))

(defn mount!
  [items]
  (init-state)
  (rum/mount (user-list items) (.getElementById js/document "content")))