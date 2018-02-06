(ns swarmpit.component.service.info.secrets
  (:require [material.component.form :as form]
            [material.component.list-table-auto :as list]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def headers ["Name" "Target" "UID" "GID" "Mode"])

(def render-item-keys
  [[:secretName] [:secretTarget] [:uid] [:gid] [:mode]])

(defn render-item
  [item]
  (val item))

(defn onclick-handler
  [item]
  (routes/path-for-frontend :secret-info {:id (:secretName item)}))

(rum/defc form < rum/static [secrets]
  (when (not-empty secrets)
    [:div.form-layout-group.form-layout-group-border
     (form/section "Secrets")
     (list/table headers
                 secrets
                 render-item
                 render-item-keys
                 onclick-handler)]))

