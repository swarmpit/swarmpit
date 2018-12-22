(ns swarmpit.component.registry.list
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [material.component.list.util :as list-util]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.storage :as storage]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [cljs.core :as core]
            [rum.core :as rum]
            [swarmpit.component.common :as common]))

(enable-console-print!)

(def render-metadata
  {:table {:summary [{:name      "Name"
                      :render-fn (fn [item] (:name item))}
                     {:name      "Url"
                      :render-fn (fn [item] (:url item))}
                     {:name      "Public"
                      :render-fn (fn [item] (if (:value item) "yes" "no"))}
                     {:name      "Secure"
                      :render-fn (fn [item] (if (:withAuth item) "yes" "no"))}]}
   :list  {:primary   (fn [item] (:name item))
           :secondary (fn [item] (:url item))}})

(defn- onclick-handler
  [item]
  (dispatch! (routes/path-for-frontend :registry-info {:id (:_id item)})))

(defn- registries-handler
  []
  (ajax/get
    (routes/path-for-backend :registries)
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:items] response state/form-value-cursor))}))

(defn form-search-fn
  [event]
  (state/update-value [:filter :query] (-> event .-target .-value) state/form-state-cursor))

(defn- init-form-state
  []
  (state/set-value {:loading? false
                    :filter   {:query ""}} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (registries-handler))))

(def form-toolbar
  {:buttons [(comp/button
               {:color "primary"
                :key   "lrett"
                :href  (routes/path-for-frontend :registry-create)}
               (html [:span.icon--left
                      (comp/svg {:key "slt"} icon/add-small)])
               "Add registry")]})

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading? filter]} (state/react state/form-state-cursor)
        filtered-items (-> (core/filter #(= (:owner %) (storage/user)) items)
                           (list-util/filter (:query filter)))]
    (progress/form
      loading?
      (common/list "Registries"
                   items
                   filtered-items
                   render-metadata
                   onclick-handler
                   form-toolbar))))
