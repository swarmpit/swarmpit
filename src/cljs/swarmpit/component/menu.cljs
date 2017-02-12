(ns swarmpit.component.menu
  (:require [swarmpit.material :as material]
            [rum.core :as rum]))

(enable-console-print!)

(defonce opened? (atom false))

(def drawer-container-closed-style #js{:width     "70px"
                                       :transform "translate(0px, 0px)"})

(def drawer-container-opened-style #js{:width "200px"})

(rum/defc drawer-category < rum/static [name opened?]
  (let [class (if opened? "drawer-item-category"
                          "drawer-item-category closed")]
    (material/menu-item #js {:className   class
                             :primaryText name
                             :disabled    true})))

(rum/defc drawer-item < rum/static [name icon opened?]
  (let [text (if opened? name "")]
    (material/menu-item #js {:className     "drawer-item"
                             :innerDivStyle #js {:paddingLeft "50px"}
                             :primaryText   text
                             :leftIcon      icon})))

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
                       (drawer-category "BUILD" opened)
                       (drawer-item "Repositories" material/repositories-icon opened)
                       (drawer-category "APPLICATIONS" opened)
                       (drawer-item "Stacks" material/stacks-icon opened)
                       (drawer-item "Services" material/services-icon opened)
                       (drawer-item "Containers" material/containers-icon opened)
                       (drawer-category "INFRASTRUCTURE" opened)
                       (drawer-item "Nodes" material/nodes-icon opened)
                       (drawer-item "Networks" material/networks-icon opened)))))