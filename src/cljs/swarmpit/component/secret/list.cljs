(ns swarmpit.component.secret.list
  (:require [material.component :as comp]
            [material.component.list :as list]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.time :as time]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def render-metadata
  [{:name    "Name"
    :key     [:secretName]
    :primary true}
   {:name      "Created"
    :key       [:createdAt]
    :render-fn (fn [value _] (time/humanize value))}])

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
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-panel
          (panel/search
            "Search secrets"
            (fn [event]
              (state/update-value [:filter :query] (-> event .-target .-value) state/form-state-cursor)))
          (comp/button
            {:variant "contained"
             :onClick #(dispatch! (routes/path-for-frontend :secret-create))
             :color   "primary"} "New Secret")]
         [:div.Swarmpit-form-context
          (list/responsive-table
            render-metadata
            nil
            (->> (list/filter items (:query filter))
                 (sort-by :secretName))
            onclick-handler)]]))))