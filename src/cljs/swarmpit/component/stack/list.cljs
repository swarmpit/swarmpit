(ns swarmpit.component.stack.list
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.list.basic :as list]
            [material.component.list.util :as list-util]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [swarmpit.component.common :as common]))

(enable-console-print!)

(def render-metadata
  {:table {:summary [{:name      "Name"
                      :render-fn (fn [item] (:stackName item))}
                     {:name      "Services"
                      :render-fn (fn [item] (get-in item [:stackStats :services]))}
                     {:name      "Networks"
                      :render-fn (fn [item] (get-in item [:stackStats :networks]))}
                     {:name      "Volumes"
                      :render-fn (fn [item] (get-in item [:stackStats :volumes]))}
                     {:name      "Configs"
                      :render-fn (fn [item] (get-in item [:stackStats :configs]))}
                     {:name      "Secrets"
                      :render-fn (fn [item] (get-in item [:stackStats :secrets]))}]}
   :list  {:primary (fn [item] (:stackName item))}})

(defn- onclick-handler
  [item]
  (dispatch! (routes/path-for-frontend :stack-info {:name (:stackName item)})))

(defn- format-response
  [response]
  (map #(hash-map
          :stackName (:stackName %)
          :stackFile (:stackFile %)
          :stackStats {:services (count (:services %))
                       :networks (count (:networks %))
                       :volumes  (count (:volumes %))
                       :configs  (count (:configs %))
                       :secrets  (count (:secrets %))}) response))

(defn- stack-handler
  []
  (ajax/get
    (routes/path-for-backend :stacks)
    {:state      [:loading?]
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:items] response state/form-value-cursor))}))

(defn form-search-fn
  [event]
  (state/update-value [:filter :query] (-> event .-target .-value) state/form-state-cursor))

(def form-actions
  [{:button (comp/icon-button
              {:color   "inherit"
               :onClick #(dispatch! (routes/path-for-frontend :stack-create))} icon/add-circle)
    :name   "New stack"}])

(defn- init-form-state
  []
  (state/set-value {:loading? false
                    :filter   {:query ""}} state/form-state-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (stack-handler))))

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading? filter]} (state/react state/form-state-cursor)]
    (common/list "Stacks"
                 items
                 (->> (list-util/filter items (:query filter))
                      (format-response)
                      (sort-by :stackName))
                 render-metadata
                 onclick-handler)))
