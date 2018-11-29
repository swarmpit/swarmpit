(ns swarmpit.component.service.info.secrets
  (:require [material.component :as comp]
            [material.component.list.basic :as list]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [rum.core :as rum]))

(enable-console-print!)

(defn onclick-handler
  [item]
  (dispatch! (routes/path-for-frontend :secret-info {:id (:secretName item)})))

(def render-metadata
  {:table {:summary [{:name      "Name"
                      :render-fn (fn [item] (:secretName item))}
                     {:name      "Target"
                      :render-fn (fn [item] (:secretTarget item))}
                     {:name      "UID"
                      :render-fn (fn [item] (:uid item))}
                     {:name      "GID"
                      :render-fn (fn [item] (:gid item))}
                     {:name      "Mode"
                      :render-fn (fn [item] (:mode item))}]}
   :list  {:primary   (fn [item] (:secretName item))
           :secondary (fn [item] (:secretTarget item))}})

(rum/defc form < rum/static [secrets]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     "Secrets"})
    (comp/card-content
      {:className "Swarmpit-table-card-content"}
      (list/responsive
        render-metadata
        secrets
        onclick-handler))))

