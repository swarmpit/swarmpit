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
         [:div.Swarmpit-form-toolbar
          (comp/container
            {:maxWidth  "sm"
             :className "Swarmpit-container"}
            (comp/tabs
              {:value          active
               :onChange       (fn [_ v] (reset! active-tab v))
               :indicatorColor "primary"
               :textColor      "primary"
               :variant        "scrollable"
               :scrollButtons  "auto"
               :aria-label     "tabs"}
              (comp/tab {:label "Password change"})
              (comp/tab {:label "API Access"}))
            (comp/divider {})
            (comp/card
              {:className "Swarmpit-form-card Swarmpit-tabs Swarmpit-fcard"}
              (comp/box
                {:className "Swarmpit-fcard-header"}
                (comp/typography
                  {:className "Swarmpit-fcard-header-title"
                   :variant   "h6"
                   :component "div"}
                  (case active
                    0 "Password change"
                    1 "API Access")))
              (common/tab-panel
                {:value active
                 :index 0}
                (password/form))
              (common/tab-panel
                {:value active
                 :index 1}
                (api-access/form))))]]))))