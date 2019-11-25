(ns swarmpit.component.account-settings
  (:require [rum.core :as rum]
            [material.components :as comp]
            [swarmpit.component.password :as password]
            [swarmpit.component.api-access :as api-access]
            [swarmpit.component.common :as common]
            [sablono.core :refer-macros [html]]))

(enable-console-print!)

(defonce active-tab (atom 0))

(rum/defc form < rum/reactive []
  (let [active (rum/react active-tab)]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (comp/tabs
            {:value          active
             :onChange       (fn [_ v] (reset! active-tab v))
             :indicatorColor "primary"
             :textColor      "primary"
             :variant        "scrollable"
             :scrollButtons  "auto"
             :aria-label     "scrollable auto tabs example"}
            (comp/tab {:label "Security"})
            (comp/tab {:label "API Access"}))
          (comp/divider {})
          (common/tab-panel
            {:value active
             :index 0}
            (password/form))
          (common/tab-panel
            {:value active
             :index 1}
            (api-access/form))]]))))