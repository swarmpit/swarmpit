(ns swarmpit.component.menu
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.component.state :as state]
            [swarmpit.storage :as storage]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:layout])

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
   {:name    "Services"
    :icon    icon/services
    :handler :service-list}
   {:name    "Tasks"
    :icon    icon/tasks
    :handler :task-list}
   {:name "INFRASTRUCTURE"}
   {:name    "Networks"
    :icon    icon/networks
    :handler :network-list}
   {:name    "Nodes"
    :icon    icon/nodes
    :handler :node-list}
   {:name "DATA"}
   {:name    "Volumes"
    :icon    icon/volumes
    :handler :volume-list}
   {:name    "Secrets"
    :icon    icon/secrets
    :handler :secret-list}])

(def admin-menu
  [{:name "USERS"}
   {:name    "Dockerhub"
    :icon    icon/docker
    :handler :dockerhub-user-list}
   {:name    "Swarmpit"
    :icon    icon/users
    :handler :user-list}
   {:name "OTHER"}
   {:name    "Registries"
    :icon    icon/registries
    :handler :registry-list}])

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

(rum/defc drawer-item < rum/static [name icon handler opened? selected?]
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
       :href          (routes/path-for-frontend handler)
       :leftIcon      drawer-item-icon})))

(rum/defc drawer < rum/reactive [title]
  (let [{:keys [opened]} (state/react cursor)
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
                    handler (:handler menu-item)
                    selected (string/includes? title name)]
                (if (some? icon)
                  (drawer-item name icon handler opened selected)
                  (drawer-category name opened)))) (if (storage/admin?)
                                                     (concat menu admin-menu)
                                                     menu)))))))