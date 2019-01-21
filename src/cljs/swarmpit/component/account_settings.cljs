(ns swarmpit.component.account-settings
  (:require [rum.core :as rum]
            [material.icon :as icon]
            [material.components :as comp]
            [swarmpit.component.password :as password]
            [swarmpit.component.api-access :as api-access]
            [sablono.core :refer-macros [html]]))

(enable-console-print!)

(def active-panel (atom nil))

(rum/defc panel < rum/static [expanded type title comp]
  (comp/expansion-panel
    {:className (if (= expanded type)
                  "Swarmpit-expansion-panel Swarmpit-expansion-panel-expanded"
                  "Swarmpit-expansion-panel")
     :expanded  (= expanded type)
     :onChange  #(if (= expanded type)
                   (reset! active-panel false)
                   (reset! active-panel type))}
    (comp/expansion-panel-summary
      {:className  "Swarmpit-expansion-panel-summary"
       :expandIcon icon/expand-more}
      (comp/typography
        {:noWrap  true
         :variant "subtitle1"} title))
    (comp/expansion-panel-details
      {}
      comp)))

(rum/defc form < rum/reactive []
  (let [expanded (rum/react active-panel)]
    (comp/mui
      (html
        [:div.Swarmpit-form
         [:div.Swarmpit-form-context
          (panel expanded
                 :password
                 "Password change"
                 (password/form))
          (panel expanded
                 :api-access
                 "API access"
                 (api-access/form))]]))))





