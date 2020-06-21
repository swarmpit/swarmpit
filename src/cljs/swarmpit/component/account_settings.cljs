(ns swarmpit.component.account-settings
  (:require [rum.core :as rum]
            [material.components :as comp]
            [swarmpit.component.password :as password]
            [swarmpit.component.api-access :as api-access]
            [sablono.core :refer-macros [html]]))

(enable-console-print!)

(rum/defc form-password < rum/static []
  (comp/box
    {:my 2}
    (comp/typography {:variant   "h6"
                      :className "Swarmpit-section"} "Password change")
    (password/form)))

(rum/defc form-api-access < rum/static []
  (comp/box
    {:my 2}
    (comp/typography {:variant   "h6"
                      :className "Swarmpit-section"} "API Access")
    (api-access/form)))

(rum/defc form < rum/reactive []
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-toolbar
        (comp/container
          {:maxWidth  "sm"
           :className "Swarmpit-container"}
          (form-password)
          (form-api-access))]])))