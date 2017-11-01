(ns swarmpit.component.service.info.secrets
  (:require [material.component :as comp]
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
  (routes/path-for-frontend :secret-info (select-keys item [:id])))

(rum/defc form < rum/static [secrets]
  (when (not-empty secrets)
    [:div.form-service-view-group.form-service-group-border
     (comp/form-section "Secrets")
     (comp/list-table-auto headers
                           secrets
                           render-item
                           render-item-keys
                           onclick-handler)]))

