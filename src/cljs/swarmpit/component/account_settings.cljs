(ns swarmpit.component.account-settings
  (:require [rum.core :as rum]
            [material.components :as comp]
            [swarmpit.component.password :as password]
            [swarmpit.component.api-access :as api-access]
            [sablono.core :refer-macros [html]]
            [swarmpit.storage :as storage]))

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

(rum/defc form-appearance < rum/reactive []
  (let [mode (rum/react comp/theme-mode)]
    (comp/box
      {:my 2}
      (comp/typography {:variant   "h6"
                        :className "Swarmpit-section"} "Appearance")
      (comp/form-control-label
        {:control (comp/switch
                    {:checked  (= "dark" mode)
                     :onChange (fn [_]
                                 (let [new-mode (if (= "dark" mode) "light" "dark")]
                                   (comp/set-theme-mode! new-mode)
                                   (.reload js/window.location)))
                     :color    "primary"})
         :label   "Dark mode"}))))

(rum/defc form < rum/reactive []
  (comp/mui
    (html
      [:div.Swarmpit-form
       [:div.Swarmpit-form-toolbar
        (comp/container
          {:maxWidth  "sm"
           :className "Swarmpit-container"}
          (form-appearance)
          (form-password)
          (form-api-access))]])))