(ns swarmpit.component.dockerhub.list
  (:require [material.component :as comp]
            [material.component.panel :as panel]
            [material.component.list-table :as list]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.storage :as storage]
            [cljs.core :as core]
            [rum.core :as rum]))

(enable-console-print!)

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

(defn- users-handler
  []
  (ajax/get
    (routes/path-for-backend :dockerhub-users)
    {:progress   [:loading?]
     :on-success (fn [response]
                   (state/update-value [:items] response state/form-value-cursor))}))

(defn- init-form-state
  []
  (state/set-value {:loading? false
                    :filter   {:query ""}} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (users-handler))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading? filter]} (state/react state/form-state-cursor)
        filtered-items (-> (core/filter #(= (:owner %) (storage/user)) items)
                           (list/filter (:query filter)))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/text-field
         {:id       "filter"
          :hintText "Search hub users"
          :onChange (fn [_ v]
                      (state/update-value [:filter :query] v state/form-state-cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :dockerhub-user-create)
            :label   "New user"
            :primary true}))]]
     (list/table headers
                 (sort-by :username filtered-items)
                 loading?
                 render-item
                 render-item-keys
                 onclick-handler)]))