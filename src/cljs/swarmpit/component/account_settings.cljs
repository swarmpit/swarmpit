(ns swarmpit.component.account-settings
  (:require [rum.core :as rum]
            [material.components :as comp]
            [swarmpit.component.iam.aws :as aws]
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
         (password/form)]
        [:div.Swarmpit-form-paper (comp/divider)]
        [:div.Swarmpit-form-paper
         (common/edit-title "Cloud access")
         (aws/form)]
        [:div.Swarmpit-form-paper (comp/divider)]
        [:div.Swarmpit-form-paper
         (common/edit-title "API access")
         (api-access/form)]]])))





