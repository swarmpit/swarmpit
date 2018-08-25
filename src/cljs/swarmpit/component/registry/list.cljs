(ns swarmpit.component.registry.list
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.list :as list]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.storage :as storage]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [cljs.core :as core]
            [rum.core :as rum]))

(enable-console-print!)

(def render-metadata
  [{:name    "Name"
    :key     [:name]
    :primary true}
   {:name "Url"
    :key  [:url]}
   {:name      "Public"
    :key       [:public]
    :render-fn (fn [value _] (if value "yes" "no"))}
   {:name      "Secure"
    :key       [:withAuth]
    :render-fn (fn [value _] (if value "yes" "no"))}])

(defn- onclick-handler
  [item]
  (routes/path-for-frontend :registry-info {:id (:_id item)}))

(defn- registries-handler
  []
  (ajax/get
    (routes/path-for-backend :registries)
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
      (registries-handler))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading? filter]} (state/react state/form-state-cursor)
        filtered-items (-> (core/filter #(= (:owner %) (storage/user)) items)
                           (list/filter (:query filter)))]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-panel
          (panel/search
            "Search registries"
            (fn [event]
              (state/update-value [:filter :query] (-> event .-target .-value) state/form-state-cursor)))
          (comp/button
            {:variant "contained"
             :onClick #(dispatch! (routes/path-for-frontend :registry-create))
             :color   "primary"} "Add registry")]
         [:div.Swarmpit-form-context
          (list/responsive-table
            render-metadata
            nil
            (sort-by :name filtered-items)
            onclick-handler)]]))))