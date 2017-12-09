(ns swarmpit.component.user.list
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.panel :as panel]
            [material.component.list-table :as list]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(def cursor [:form])

(def headers [{:name  "Username"
               :width "40%"}
              {:name  "Email"
               :width "40%"}
              {:name  "Is Admin"
               :width "20%"}])

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

(defn- users-handler
  []
  (handler/get
    (routes/path-for-backend :users)
    {:on-success (fn [response]
                   (state/update-value [:items] response cursor))}))

(defn- init-state
  []
  (state/set-value {:filter {:username ""}} cursor))

(def mixin-init-state
  (mixin/init-state
    (fn []
      (init-state)
      (users-handler))))

(rum/defc form < rum/reactive
                 mixin-init-state
                 mixin/focus-filter []
  (let [{:keys [filter items]} (state/react cursor)
        filtered-items (filter-items items (:username filter))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/text-field
         {:id       "filter"
          :hintText "Filter by username"
          :onChange (fn [_ v]
                      (state/update-value [:filter :username] v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :user-create)
            :label   "New user"
            :primary true}))]]
     (list/table headers
                 (sort-by :username filtered-items)
                 (nil? items)
                 render-item
                 render-item-keys
                 onclick-handler)]))