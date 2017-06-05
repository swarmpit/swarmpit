(ns swarmpit.component.header
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:menu])

(def user-avatar-style
  {:marginRight "10px"
   :border      "1px solid #fff"})

(def appbar-style
  {:position  "fixed"
   :boxShadow "none"
   :top       0})

(def appbar-title-style
  {:fontSize   "20px"
   :fontWeight 200})

(def appbar-icon-style
  {:position "fixed"
   :right    20})

(defn- user-avatar []
  (comp/avatar
    {:style user-avatar-style
     :src   "https://www.gravatar.com/avatar/6e6fd910c0594f4f2b448e3530eb5abd"}))

(defn- user-menu-button []
  (comp/icon-button
    nil
    (comp/svg
      {:key   "user-menu-button-icon"
       :color "#fff"}
      icon/expand)))

(defn- user-menu []
  (comp/icon-menu
    {:iconButtonElement (user-menu-button)}
    (comp/menu-item
      {:key         "user-menu-settings"
       :primaryText "Change password"})
    (comp/menu-item
      {:key         "user-menu-logout"
       :onTouchTap  (fn []
                      (storage/remove "token")
                      (dispatch! "/#/login"))
       :primaryText "Log out"})))

(rum/defc userbar < rum/static []
  [:div.user-bar
   (user-avatar)
   [:span "admin"]
   (user-menu)])

(rum/defc appbar < rum/reactive []
  (let [{:keys [domain]} (state/react cursor)]
    (comp/mui
      (comp/app-bar
        {:title              domain
         :titleStyle         appbar-title-style
         :style              appbar-style
         :iconElementRight   (userbar)
         :iconStyleRight     appbar-icon-style
         :showMenuIconButton false}))))