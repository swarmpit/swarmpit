(ns swarmpit.component.service.info.configs
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.list.info :as list]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def render-metadata
  [{:name    "Name"
    :primary true
    :key     [:configName]}
   {:name "Target"
    :key  [:configTarget]}
   {:name "UID"
    :key  [:uid]}
   {:name "GID"
    :key  [:gid]}
   {:name "Mode"
    :key  [:mode]}])

(defn onclick-handler
  [item]
  (routes/path-for-frontend :config-info {:id (:configName item)}))

(rum/defc form < rum/static [configs]
  (comp/card
    {:className "Swarmpit-form-card"}
    (comp/card-header
      {:className "Swarmpit-form-card-header"
       :subheader (form/subheader "Configs" icon/settings)})
    (comp/card-content
      {}
      (list/table
        render-metadata
        configs
        onclick-handler))))

