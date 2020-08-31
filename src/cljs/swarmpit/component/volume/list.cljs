(ns swarmpit.component.volume.list
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [material.component.list.util :as list-util]
            [swarmpit.component.common :as common]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def render-metadata
  {:table {:summary [{:name      "Name"
                      :render-fn (fn [item] (:volumeName item))}
                     {:name      "Driver"
                      :render-fn (fn [item] (:driver item))}]}
   :list  {:primary   (fn [item] (:volumeName item))
           :secondary (fn [item] (:driver item))}})

(defn onclick-handler
  [item]
  (routes/path-for-frontend :volume-info {:name (:volumeName item)}))

(defn- volumes-handler
  []
  (ajax/get
    (routes/path-for-backend :volumes)
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
      (volumes-handler))))

(def toolbar-render-metadata
  [{:name     "New volume"
    :onClick  #(dispatch! (routes/path-for-frontend :volume-create))
    :primary  true
    :icon     (icon/add-circle-out)
    :icon-alt (icon/add)}])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form
                 mixin/focus-filter [_]
  (let [{:keys [items]} (state/react state/form-value-cursor)
        {:keys [loading?]} (state/react state/form-state-cursor)
        {:keys [query]} (state/react state/search-cursor)
        filtered-items (->> (list-util/filter items query)
                            (sort-by :volumeName))]
    (progress/form
      loading?
      (common/list "Volumes"
                   items
                   filtered-items
                   render-metadata
                   onclick-handler
                   toolbar-render-metadata))))
