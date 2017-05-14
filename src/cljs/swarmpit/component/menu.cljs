(ns swarmpit.component.menu
  (:require [swarmpit.material :as material :refer [svg]]
            [swarmpit.component.state :as state]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:menu])

(def drawer-container-closed-style
  #js{:width     "70px"
      :transform "translate(0px, 0px)"})

(def drawer-container-opened-style
  #js{:width "200px"})

(rum/defc drawer-category < rum/static [name opened?]
  (let [class (if opened? "drawer-item-category"
                          "drawer-item-category closed")]
    (material/menu-item
      #js {:className   class
           :primaryText name
           :disabled    true})))

(rum/defc drawer-item < rum/static [name icon opened? selected-domain]
  (let [text (if opened? name nil)
        lname (string/lower-case name)
        class (if (= name selected-domain)
                "drawer-item-selected"
                "drawer-item")
        licon (if (= name selected-domain)
                (svg #js {:color "#437f9d"} icon)
                (svg icon))]
    (material/menu-item
      #js {:className     class
           :innerDivStyle #js {:paddingLeft "50px"}
           :primaryText   text
           :href          (str "/#/" lname)
           :leftIcon      licon})))

(rum/defc drawer < rum/reactive []
  (let [{:keys [opened domain]} (state/react cursor)
        drawer-container-style (if opened
                                 drawer-container-opened-style
                                 drawer-container-closed-style)
        drawer-appbar-icon (if opened
                             (material/icon-button nil (svg material/view-compact-icon))
                             (material/icon-button nil (svg material/view-confy-icon)))]
    (material/theme
      (material/drawer
        #js {:open               opened
             :containerStyle     drawer-container-style
             :containerClassName "drawer-container"}
        (material/app-bar
          #js{:className                "drawer-appbar"
              :iconElementLeft          drawer-appbar-icon
              :onLeftIconButtonTouchTap (fn []
                                          (state/update-value :opened (not opened) cursor))})
        (material/menu #js {:style #js {:height   "100%"
                                        :overflow "auto"}}
                       (drawer-category "BUILD" opened)
                       (drawer-item "Repositories" material/repositories-icon opened domain)
                       (drawer-category "APPLICATIONS" opened)
                       ;(drawer-item "Stacks" material/stacks-icon opened)
                       (drawer-item "Services" material/services-icon opened domain)
                       (drawer-item "Tasks" material/containers-icon opened domain)
                       (drawer-category "INFRASTRUCTURE" opened)
                       (drawer-item "Nodes" material/nodes-icon opened domain)
                       (drawer-item "Networks" material/networks-icon opened domain))))))