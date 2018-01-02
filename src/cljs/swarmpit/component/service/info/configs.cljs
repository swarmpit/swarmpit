(ns swarmpit.component.service.info.configs
  (:require [material.component.form :as form]
            [material.component.list-table-auto :as list]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def headers ["Name" "Target" "UID" "GID" "Mode"])

(def render-item-keys
  [[:configName] [:configTarget] [:uid] [:gid] [:mode]])

(defn render-item
  [item]
  (val item))

(defn onclick-handler
  [item]
  (routes/path-for-frontend :config-info (select-keys item [:id])))

(rum/defc form < rum/static [configs]
  (when (not-empty configs)
    [:div.form-layout-group.form-layout-group-border
     (form/section "Configs")
     (list/table headers
                 configs
                 render-item
                 render-item-keys
                 onclick-handler)]))

