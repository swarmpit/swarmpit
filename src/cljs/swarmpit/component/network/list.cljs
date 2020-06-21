(ns swarmpit.component.network.list
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [material.component.list.util :as list-util]
            [material.component.label :as label]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [swarmpit.component.common :as common]))

(enable-console-print!)

(def render-metadata
  {:table {:summary [{:name      "Name"
                      :render-fn (fn [item] (:networkName item))}
                     {:name      "Driver"
                      :render-fn (fn [item] (:driver item))}
                     {:name      "Subnet"
                      :render-fn (fn [item] (get-in item [:ipam :subnet]))}
                     {:name      "Gateway"
                      :render-fn (fn [item] (get-in item [:ipam :gateway]))}]}
   :list  {:primary   (fn [item] (:networkName item))
           :secondary (fn [item] (:driver item))}})

(defn onclick-handler
  [item]
  (routes/path-for-frontend :network-info {:id (:networkName item)}))

(defn- networks-handler
  []
  (ajax/get
    (routes/path-for-backend :networks)
    {:state      [:loading?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (state/update-value [:items] response state/form-value-cursor)))}))

(defn form-search-fn
  [event]
  (state/update-value [:query] (-> event .-target .-value) state/search-cursor))

(defn- init-form-state
  []
  (state/set-value {:loading? false} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (networks-handler))))

(def toolbar-render-metadata
  [{:name     "New network"
    :onClick  #(dispatch! (routes/path-for-frontend :network-create))
    :primary  true
    :icon     (icon/add-circle-out)
    :icon-alt (icon/add)}])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [query]} (state/react state/search-cursor)
        {:keys [loading?]} (state/react state/form-state-cursor)
        filtered-items (->> (list-util/filter items query)
                            (sort-by :created)
                            (reverse))]
    (progress/form
      loading?
      (common/list "Networks"
                   items
                   filtered-items
                   render-metadata
                   onclick-handler
                   toolbar-render-metadata))))
