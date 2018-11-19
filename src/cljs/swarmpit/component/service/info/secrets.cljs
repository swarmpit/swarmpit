(ns swarmpit.component.service.info.secrets
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.list.info :as list]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(defn onclick-handler
  [item]
  (routes/path-for-frontend :secret-info {:id (:secretName item)}))

(def render-metadata
  [{:name    "Name"
    :primary true
    :key     [:secretName]}
   {:name "Target"
    :key  [:secretTarget]}
   {:name "UID"
    :key  [:uid]}
   {:name "GID"
    :key  [:gid]}
   {:name "Mode"
    :key  [:mode]}])

(rum/defc form < rum/static [secrets]
  (comp/card
    {:className "Swarmpit-form-card"}
    (comp/card-header
      {:className "Swarmpit-form-card-header"
       :subheader (form/subheader "Secrets" icon/settings)})
    (comp/card-content
      {}
      (list/table
        render-metadata
        secrets
        onclick-handler))))

