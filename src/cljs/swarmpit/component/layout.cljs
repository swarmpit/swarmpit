(ns swarmpit.component.layout
  (:require [swarmpit.material :as material]
            [rum.core :as rum]))

(enable-console-print!)

(defonce opened? (atom false))

(def drawer-container-closed-style #js{:width     "70px"
                                       :transform "translate(0px, 0px)"})

(def drawer-container-opened-style #js{:width "256px"})

(rum/defc appbar < rum/static []
  (material/theme
    (material/app-bar #js{:className          "appbar"
                          :showMenuIconButton false})))

(rum/defc drawer < rum/reactive []
  (let [opened (rum/react opened?)
        drawer-container-style (if (true? opened)
                                 drawer-container-opened-style
                                 drawer-container-closed-style)
        drawer-appbar-icon (if (true? opened)
                             (material/icon-button #js {} material/view-compact-icon)
                             (material/icon-button #js {} material/view-confy-icon))]
    (material/theme
      (material/drawer #js {:open               opened
                            :containerStyle     drawer-container-style
                            :containerClassName "drawer-container"}
                       (material/app-bar #js{:className                "drawer-appbar"
                                             :iconElementLeft          drawer-appbar-icon
                                             :onLeftIconButtonTouchTap (fn [] (reset! opened? (not @opened?)))})
                       (material/menu-item #js {:className   "drawer-item-category"
                                                :primaryText "BUILD"
                                                :disabled    true})
                       (material/menu-item #js {:className   "drawer-item"
                                                :primaryText "Repositories"
                                                :leftIcon    material/repositories-icon})
                       (material/menu-item #js {:className   "drawer-item-category"
                                                :primaryText "APPLICATIONS"
                                                :disabled    true})
                       (material/menu-item #js {:className   "drawer-item"
                                                :primaryText "Stacks"
                                                :leftIcon    material/stacks-icon})
                       (material/menu-item #js {:className   "drawer-item"
                                                :primaryText "Services"
                                                :leftIcon    material/services-icon})
                       (material/menu-item #js {:className   "drawer-item"
                                                :primaryText "Containers"
                                                :leftIcon    material/containers-icon})
                       (material/menu-item #js {:className   "drawer-item-category"
                                                :primaryText "INFRASTRUCTURE"
                                                :disabled    true})
                       (material/menu-item #js {:className   "drawer-item"
                                                :primaryText "Nodes"
                                                :leftIcon    material/nodes-icon})
                       ))))

(rum/defc layout < rum/reactive []
  (let [opened (rum/react opened?)
        layout-style (if (true? opened)
                       "layout-opened"
                       "layout-closed")]
    [:div {:class ["layout" layout-style]}
     [:header (appbar)]
     [:nav (drawer)]
     [:main [:div#content]]]))

(defn mount!
  []
  (rum/mount (layout) (.getElementById js/document "layout")))