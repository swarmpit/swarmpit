(ns swarmpit.component.secret.list
  (:require [material.component :as comp]
            [material.component.panel :as panel]
            [material.component.list-table :as list]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.time :as time]
            [rum.core :as rum]))

(enable-console-print!)

(def headers [{:name  "Name"
               :width "50%"}
              {:name  "Created"
               :width "50%"}])

(def render-item-keys
  [[:secretName] [:createdAt]])

(defn- render-item
  [item _]
  (case (key item)
    :createdAt (time/humanize (val item))
    (val item)))

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :secret-info {:id (:secretName item)}))

(defn- secrets-handler
  []
  (ajax/get
    (routes/path-for-backend :secrets)
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:items] response state/form-value-cursor))}))

(defn- init-form-state
  []
  (state/set-value {:loading? false
                    :filter   {:query ""}} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (secrets-handler))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading? filter]} (state/react state/form-state-cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/text-field
         {:id       "filter"
          :hintText "Search secrets"
          :onChange (fn [_ v]
                      (state/update-value [:filter :query] v state/form-state-cursor))})]
      [:div.form-panel-right
       (comp/mui
         (comp/raised-button
           {:href    (routes/path-for-frontend :secret-create)
            :label   "New secret"
            :primary true}))]]
     (list/table headers
                 (->> (list/filter items (:query filter))
                      (sort-by :secretName))
                 loading?
                 render-item
                 render-item-keys
                 onclick-handler)]))