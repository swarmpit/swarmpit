(ns swarmpit.component.service.info.configs
  (:require [material.icon :as icon]
            [material.components :as comp]
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

(rum/defc form < rum/static [configs service-id]
  (comp/card
    {:className "Swarmpit-card"
     :key       "scc"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :key       "scch"
       :title     "Configs"
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :href       (routes/path-for-frontend
                                   :service-edit
                                   {:id service-id}
                                   {:section "Configs"})}
                    (comp/svg icon/edit))})
    (comp/card-content
      {:className "Swarmpit-table-card-content"
       :key       "sccc"}
      (rum/with-key
        (list/responsive
          render-metadata
          configs
          onclick-handler)
        "scccrl"))))

