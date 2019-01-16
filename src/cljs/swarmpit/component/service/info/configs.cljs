(ns swarmpit.component.service.info.configs
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.list.basic :as list]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn onclick-handler
  [item]
  (dispatch! (routes/path-for-frontend :config-info {:id (:configName item)})))

(def render-metadata
  {:primary   (fn [item] (:configName item))
   :secondary (fn [item] (:configTarget item))})

(rum/defc form < rum/static [configs service-id]
  (comp/card
    {:className "Swarmpit-card"
     :key       "scc"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :key       "scch"
       :title     (comp/typography
                    {:variant "h6"
                     :key     "configs-title"} "Configs")
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :href       (routes/path-for-frontend
                                   :service-edit
                                   {:id service-id}
                                   {:section "Configs"})}
                    (comp/svg icon/edit-path))})
    (if (empty? configs)
      (comp/card-content
        {:key "sccce"}
        (html [:div "No configs defined for the service."]))
      (comp/card-content
        {:className "Swarmpit-table-card-content"
         :key       "sccc"}
        (rum/with-key
          (list/list
            render-metadata
            configs
            onclick-handler)
          "scccrl")))))

