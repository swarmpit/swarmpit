(ns swarmpit.component.service.info.configs
  (:require [material.component :as comp]
            [material.component.list.basic :as list]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [rum.core :as rum]))

(enable-console-print!)

(defn onclick-handler
  [item]
  (dispatch! (routes/path-for-frontend :config-info {:id (:configName item)})))

(def render-metadata
  {:table {:summary [{:name      "Name"
                      :render-fn (fn [item] (:configName item))}
                     {:name      "Target"
                      :render-fn (fn [item] (:configTarget item))}
                     {:name      "UID"
                      :render-fn (fn [item] (:uid item))}
                     {:name      "GID"
                      :render-fn (fn [item] (:gid item))}
                     {:name      "Mode"
                      :render-fn (fn [item] (:mode item))}]}
   :list  {:primary   (fn [item] (:secretName item))
           :secondary (fn [item] (:secretTarget item))}})

(rum/defc form < rum/static [configs]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     "Configs"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (list/responsive
        render-metadata
        configs
        onclick-handler))))

