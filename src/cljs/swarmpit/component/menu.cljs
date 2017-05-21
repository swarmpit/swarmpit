(ns swarmpit.component.menu
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.component.state :as state]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:menu])

(def drawer-container-closed-style
  {:width     "70px"
   :transform "translate(0px, 0px)"})

(def drawer-container-opened-style
  {:width "200px"})

(rum/defc drawer-category < rum/static [name opened?]
  (let [class (if opened? "drawer-item-category"
                          "drawer-item-category closed")]
    (comp/menu-item
      {:className   class
       :primaryText name
       :disabled    true})))

(rum/defc drawer-item < rum/static [name icon opened? selected-domain]
  (let [text (if opened? name nil)
        lname (string/lower-case name)
        class (if (= name selected-domain)
                "drawer-item-selected"
                "drawer-item")
        licon (if (= name selected-domain)
                (comp/svg {:color "#437f9d"} icon)
                (comp/svg icon))]
    (comp/menu-item
      {:className     class
       :innerDivStyle {:paddingLeft "50px"}
       :primaryText   text
       :href          (str "/#/" lname)
       :leftIcon      licon})))

(rum/defc drawer < rum/reactive []
  (let [{:keys [opened domain]} (state/react cursor)
        drawer-container-style (if opened
                                 drawer-container-opened-style
                                 drawer-container-closed-style)
        drawer-appbar-icon (if opened
                             (comp/icon-button nil (comp/svg icon/view-compact))
                             (comp/icon-button nil (comp/svg icon/view-confy)))]
    (comp/mui
      (comp/drawer
        {:open               opened
         :containerStyle     drawer-container-style
         :containerClassName "drawer-container"}
        (comp/app-bar
          {:className                "drawer-appbar"
           :iconElementLeft          drawer-appbar-icon
           :onLeftIconButtonTouchTap (fn []
                                       (state/update-value :opened (not opened) cursor))})
        (comp/menu
          {:style {:height   "100%"
                   :overflow "auto"}}
          (drawer-category "BUILD" opened)
          (drawer-item "Repositories" icon/repositories opened domain)
          (drawer-item "Images" icon/images opened domain)
          (drawer-category "APPLICATIONS" opened)
          (drawer-item "Services" icon/services opened domain)
          (drawer-item "Tasks" icon/containers opened domain)
          (drawer-category "INFRASTRUCTURE" opened)
          (drawer-item "Nodes" icon/nodes opened domain)
          (drawer-item "Networks" icon/networks opened domain))))))