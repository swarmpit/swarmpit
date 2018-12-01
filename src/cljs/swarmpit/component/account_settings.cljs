(ns swarmpit.component.account-settings
  (:require [rum.core :as rum]
            [swarmpit.component.password :as password]
            [sablono.core :refer-macros [html]]
            [swarmpit.component.api-access :as api-access]
            [material.components :as comp]))

(enable-console-print!)

(rum/defc form < rum/reactive []
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-context
        (comp/grid
          {:container true
           :spacing   40}
          (password/form)
          (api-access/form))]])))

