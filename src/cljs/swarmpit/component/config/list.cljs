(ns swarmpit.component.config.list
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [material.component.list.util :as list-util]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.progress :as progress]
            [swarmpit.ajax :as ajax]
            [swarmpit.time :as time]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]
            [swarmpit.component.common :as common]))

(enable-console-print!)

(defn- render-item-name [item]
  (html
    [:div
     [:div
      [:span [:b (:serviceName item)]]]
     [:div
      [:span (get-in item [:repository :image])]]]))

(def render-metadata
  {:table {:summary [{:name      "Name"
                      :render-fn (fn [item] (:configName item))}
                     {:name      "Created"
                      :render-fn (fn [item] (time/humanize (:createdAt item)))}]}
   :list  {:primary   (fn [item] (:configName item))
           :secondary (fn [item] (time/humanize (:createdAt item)))}})

(defn onclick-handler
  [item]
  (dispatch! (routes/path-for-frontend :config-info {:id (:configName item)})))

(defn- configs-handler
  []
  (ajax/get
    (routes/path-for-backend :configs)
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
      (configs-handler))))

(def toolbar-render-metadata
  {:actions [{:name     "New Config"
              :onClick  #(dispatch! (routes/path-for-frontend :config-create))
              :icon     icon/add-circle-out
              :icon-alt icon/add}]})

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading? filter]} (state/react state/form-state-cursor)]
    (progress/form
      loading?
      (common/list "Configs"
                   items
                   (->> (list-util/filter items (:query filter))
                        (sort-by :configName))
                   render-metadata
                   onclick-handler
                   toolbar-render-metadata))))
