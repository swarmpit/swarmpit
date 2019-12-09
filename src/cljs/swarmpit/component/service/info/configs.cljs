(ns swarmpit.component.service.info.configs
  (:require [material.icon :as icon]
            [material.components :as comp]
            [material.component.form :as form]
            [material.component.list.basic :as list]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch!]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(defn onclick-handler
  [item]
  (routes/path-for-frontend :config-info {:id (:configName item)}))

(def render-metadata
  {:primary   (fn [item] (:configName item))
   :secondary (fn [item] (:configTarget item))})

(rum/defc form < rum/static [configs service-id immutable?]
  (comp/card
    {:className "Swarmpit-card"}
    (comp/card-header
      {:className "Swarmpit-table-card-header"
       :title     (comp/typography {:variant "h6"} "Configs")
       :action    (comp/icon-button
                    {:aria-label "Edit"
                     :disabled   immutable?
                     :href       (routes/path-for-frontend
                                   :service-edit
                                   {:id service-id}
                                   {:section 2})}
                    (comp/svg icon/edit-path))})
    (if (empty? configs)
      (comp/card-content
        {}
        (form/item-info "No configs defined for the service."))
      (comp/card-content
        {:className "Swarmpit-table-card-content"}
        (list/list
          render-metadata
          configs
          onclick-handler)))))

