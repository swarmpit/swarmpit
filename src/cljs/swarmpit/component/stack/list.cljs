(ns swarmpit.component.stack.list
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.list.basic :as list]
            [material.component.list.util :as list-util]
            [material.component.panel :as panel]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def render-metadata
  [{:name    "Name"
    :key     [:stackName]
    :primary true}
   {:name "Services"
    :key  [:stackStats :services]}
   {:name "Networks"
    :key  [:stackStats :networks]}
   {:name "Volumes"
    :key  [:stackStats :volumes]}
   {:name "Configs"
    :key  [:stackStats :configs]}
   {:name "Secrets"
    :key  [:stackStats :secrets]}])

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
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-panel
          (panel/search
            "Search stacks"
            (fn [event]
              (state/update-value [:filter :query] (-> event .-target .-value) state/form-state-cursor)))
          (comp/button
            {:variant "contained"
             :onClick #(dispatch! (routes/path-for-frontend :stack-create))
             :color   "primary"} "New stack")]
         [:div.Swarmpit-form-context
          (list/responsive
            render-metadata
            nil
            (->> (list-util/filter items (:query filter))
                 (format-response)
                 (sort-by :stackName))
            onclick-handler)]]))))