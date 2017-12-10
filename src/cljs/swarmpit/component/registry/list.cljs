(ns swarmpit.component.registry.list
  (:require [material.component :as comp]
            [material.component.panel :as panel]
            [material.component.list-table :as list]
            [material.icon :as icon]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.storage :as storage]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(def cursor [:form])

(def headers [{:name  "Name"
               :width "30%"}
              {:name  "Url"
               :width "50%"}
              {:name  "Public"
               :width "10%"}
              {:name  "Secure"
               :width "10%"}])

(def render-item-keys
  [[:name] [:url] [:public] [:withAuth]])

(defn- render-item
  [item _]
  (let [value (val item)]
    (case (key item)
      :withAuth (if value
                  (comp/svg icon/ok)
                  "")
      :public (if value
                "yes"
                "no")
      value)))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :registry-info {:id (:_id item)}))

(defn- filter-items
  [items predicate]
  (filter #(and (string/includes? (:name %) predicate)
                (= (:owner %)
                   (storage/user))) items))

(defn- registries-handler
  []
  (handler/get
    (routes/path-for-backend :registries)
    {:on-success (fn [response]
                   (state/update-value [:items] response cursor))}))

(defn- init-state
  []
  (state/set-value {:filter {:name ""}} cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-state)
      (registries-handler))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [filter items]} (state/react cursor)
        filtered-items (filter-items items (:name filter))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/text-field
         {:id       "filter"
          :hintText "Filter by name"
          :onChange (fn [_ v]
                      (state/update-value [:filter :name] v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :registry-create)
            :label   "New registry"
            :primary true}))]]
     (list/table headers
                 (sort-by :name filtered-items)
                 (nil? items)
                 render-item
                 render-item-keys
                 onclick-handler)]))