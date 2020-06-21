(ns swarmpit.component.stack.list
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [material.component.list.util :as list-util]
            [material.component.label :as label]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [swarmpit.component.common :as common]))

(enable-console-print!)

(defn- render-status [{:keys [state]}]
  (html
    [:span.Swarmpit-table-status
     (case state
       "deployed" (label/base state "green")
       "inactive" (label/base state "info"))]))

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
                      :render-fn (fn [item] (get-in item [:stackStats :secrets]))}
                     {:name      ""
                      :status    true
                      :render-fn (fn [item] (render-status item))}]}
   :list  {:primary   (fn [item] (:stackName item))
           :status-fn (fn [item] (render-status item))}})

(defn onclick-handler
  [item]
  (case (:state item)
    "deployed" (routes/path-for-frontend :stack-info {:name (:stackName item)})
    "inactive" (routes/path-for-frontend :stack-activate {:name (:stackName item)})))

(defn- format-response
  [response]
  (map #(hash-map
          :stackName (:stackName %)
          :state (:state %)
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
      (stack-handler))))

(def toolbar-render-metadata
  [{:name     "New stack"
    :onClick  #(dispatch! (routes/path-for-frontend :stack-create))
    :primary  true
    :icon     (icon/add-circle-out)
    :icon-alt (icon/add)}])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [query]} (state/react state/search-cursor)
        {:keys [loading?]} (state/react state/form-state-cursor)]
    (progress/form
      loading?
      (common/list "Stacks"
                   items
                   (->> (list-util/filter items query)
                        (format-response)
                        (sort-by :stackName))
                   render-metadata
                   onclick-handler
                   toolbar-render-metadata))))
