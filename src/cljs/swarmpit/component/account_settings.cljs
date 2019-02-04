(ns swarmpit.component.account-settings
  (:require [rum.core :as rum]
            [material.components :as comp]
            [swarmpit.component.password :as password]
            [swarmpit.component.api-access :as api-access]
            [swarmpit.component.common :as common]
            [sablono.core :refer-macros [html]]))

(enable-console-print!)

(rum/defc form < rum/reactive []
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        [:div.Swarmpit-form-paper
         (common/edit-title "Password change")
         (comp/divider {:className "Swarmpit-form-title-divider"})
         (password/form)]
        [:div.Swarmpit-form-paper
         (common/edit-title "API access")
         (comp/divider {:className "Swarmpit-form-title-divider"})
         (api-access/form)]]])))





