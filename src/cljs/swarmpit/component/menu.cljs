(ns swarmpit.component.menu
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.component.state :as state]
            [swarmpit.storage :as storage]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:menu])

(def drawer-style
  {:boxShadow "none"})

(def drawer-container-style
  {:boxShadow   "none"
   :borderRight "1px solid #e0e4e7"
   :overflow    "hidden"})

(def drawer-container-closed-style
  (merge drawer-container-style
         {:width     "70px"
          :transform "translate(0px, 0px)"}))

(def drawer-container-opened-style
  (merge drawer-container-style
         {:width "200px"}))

(def drawer-opened-icon
  (comp/icon-button nil (comp/svg {:key "doi"} icon/view-compact)))

(def drawer-closed-icon
  (comp/icon-button nil (comp/svg {:key "dci"} icon/view-confy)))

(def drawer-item-inner-style
  {:paddingLeft "50px"})

(def drawer-item-style
  {:paddingLeft "5px"
   :fontWeight  "lighter"
   :color       "rgb(117, 117, 117)"})

(def drawer-item-selected-style
  {:paddingLeft "5px"
   :fontWeight  "normal"
   :color       "#437f9d"})

(def drawer-category-style
  {:cursor "default"})

(def drawer-category-closed-style
  (merge drawer-category-style
         {:opacity 0}))

(def menu
  [{:name "APPLICATIONS"}
   {:name "Services"
    :icon icon/services}
   {:name "Tasks"
    :icon icon/tasks}
   {:name "INFRASTRUCTURE"}
   {:name "Nodes"
    :icon icon/nodes}
   {:name "Networks"
    :icon icon/networks}])

(def admin-menu
  [{:name "SETTINGS"}
   {:name "Dockerhub"
    :icon icon/dockerhub}
   {:name "Registries"
    :icon icon/registries}
   {:name "Users"
    :icon icon/users}])

(def menu-style
  {:height   "100%"
   :overflow "auto"})

(rum/defc drawer-category < rum/static [name opened?]
  (let [drawer-category-style (if opened?
                                drawer-category-style
                                drawer-category-closed-style)]
    (comp/menu-item
      {:style       drawer-category-style
       :primaryText name
       :disabled    true})))

(rum/defc drawer-item < rum/static [name icon opened? selected?]
  (let [drawer-item-text (if opened?
                           name
                           nil)
        drawer-item-style (if selected?
                            drawer-item-selected-style
                            drawer-item-style)
        drawer-item-icon (if selected?
                           (comp/svg {:color "#437f9d"} icon)
                           (comp/svg icon))]
    (comp/menu-item
      {:style         drawer-item-style
       :innerDivStyle drawer-item-inner-style
       :primaryText   drawer-item-text
       :href          (str "/#/" (string/lower-case name))
       :leftIcon      drawer-item-icon})))

(rum/defc drawer < rum/reactive []
  (let [{:keys [opened domain]} (state/react cursor)
        drawer-container-style (if opened
                                 drawer-container-opened-style
                                 drawer-container-closed-style)
        drawer-icon (if opened
                      drawer-opened-icon
                      drawer-closed-icon)]
    (comp/mui
      (comp/drawer
        {:key            "menu-drawer"
         :open           opened
         :containerStyle drawer-container-style}
        (comp/app-bar
          {:key                      "menu-drawer-bar"
           :style                    drawer-style
           :iconElementLeft          drawer-icon
           :onLeftIconButtonTouchTap (fn []
                                       (state/update-value [:opened] (not opened) cursor))})
        (comp/menu
          {:key   "menu"
           :style menu-style}
          (map
            (fn [menu-item]
              (let [icon (:icon menu-item)
                    name (:name menu-item)
                    selected (string/includes? domain name)]
                (if (some? icon)
                  (drawer-item name icon opened selected)
                  (drawer-category name opened)))) (if (storage/admin?)
                                                     (concat menu admin-menu)
                                                     menu)))))))