(ns swarmpit.component.volume.list
  (:require [material.component :as comp]
            [material.component.panel :as panel]
            [material.component.list-table :as list]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(def headers [{:name  "Name"
               :width "50%"}
              {:name  "Driver"
               :width "50%"}])

(def render-item-keys
  [[:volumeName] [:driver]])

(defonce loading? (atom false))

(defn- render-item
  [item _]
  (val item))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :volume-info {:name (:volumeName item)}))

(defn- volumes-handler
  []
  (handler/get
    (routes/path-for-backend :volumes)
    {:state      loading?
     :on-success (fn [response]
                   (state/update-value [:items] response cursor))}))

(defn- init-state
  []
  (state/set-value {:filter {:query ""}} cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-state)
      (volumes-handler))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [filter items]} (state/react cursor)
        filtered-items (list/filter items (:query filter))]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/text-field
         {:id       "filter"
          :hintText "Search volumes"
          :onChange (fn [_ v]
                      (state/update-value [:filter :query] v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :volume-create)
            :label   "New volume"
            :primary true}))]]
     (list/table headers
                 (sort-by :volumeName filtered-items)
                 (rum/react loading?)
                 render-item
                 render-item-keys
                 onclick-handler)]))