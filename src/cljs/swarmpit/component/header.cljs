(ns swarmpit.component.header
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:menu])

(defn- avatar []
  (comp/avatar
    {:className "user-avatar"
     :src       "https://www.gravatar.com/avatar/6e6fd910c0594f4f2b448e3530eb5abd"}))

(defn- menu-button []
  (comp/icon-button
    nil
    (comp/svg
      {:color "#fff"}
      icon/expand)))

(defn- menu []
  (comp/icon-menu
    {:iconButtonElement (menu-button)}
    (comp/menu-item
      {:primaryText "Settings"})
    (comp/menu-item
      {:primaryText "Log out"})))

(rum/defc userbar < rum/static []
  [:div.user-bar
   (avatar)
   [:span "admin"]
   (menu)])

(rum/defc appbar < rum/reactive []
  (let [{:keys [domain]} (state/react cursor)]
    (comp/mui
      (comp/app-bar
        {:title              domain
         :titleStyle         {:fontSize   "20px"
                              :fontWeight 200}
         :style              {:position "fixed"
                              :top      0}
         :iconElementRight   (userbar)
         :iconStyleRight     {:position "fixed"
                              :right    20}
         :showMenuIconButton false}))))