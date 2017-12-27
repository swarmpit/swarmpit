(ns swarmpit.component.dockerhub.list
  (:require [material.component :as comp]
            [material.component.panel :as panel]
            [material.component.list-table :as list]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(def cursor [:form])

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
  (handler/get
    (routes/path-for-backend :dockerhub-users)
    {:on-success (fn [response]
                   (state/update-value [:items] response cursor))}))

(defn- init-state
  []
  (state/set-value {:filter {:query ""}} cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-state)
      (users-handler))))

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
          :hintText "Search hub users"
          :onChange (fn [_ v]
                      (state/update-value [:filter :query] v cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :dockerhub-user-create)
            :label   "New user"
            :primary true}))]]
     (list/table headers
                 (sort-by :username filtered-items)
                 (nil? items)
                 render-item
                 render-item-keys
                 onclick-handler)]))