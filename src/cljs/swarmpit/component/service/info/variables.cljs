(ns swarmpit.component.service.info.variables
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.list.info-kv :as list]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc form < rum/static [variables]
  (comp/card
    {:className "Swarmpit-form-card"}
    (comp/card-header
      {:className "Swarmpit-form-card-header"
       :title "Environment variables"
       ;:subheader (form/subheader "Environment variables" icon/settings)
       })
    (comp/card-content
      {}
      (list/list variables))))
