(ns swarmpit.component.header
  (:require [material.component :as comp]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.event.source :as eventsource]
            [swarmpit.routes :as routes]
            [clojure.string :as string]
            [rum.core :as rum]
            [goog.crypt :as crypt])
  (:import [goog.crypt Md5]))

(enable-console-print!)

(def user-avatar-style
  {:marginRight "10px"
   :border      "1px solid #fff"})

(def appbar-style
  {:position  "fixed"
   :boxShadow "none"
   :top       0})

(def appbar-icon-style
  {:position "fixed"
   :right    20})

(def appbar-title-style
  {:fontSize   "25px"
   :fontWeight 200})

(defn user-gravatar-hash [email]
  (let [md5 (Md5.)]
    (.update md5 (string/trim email))
    (crypt/byteArrayToHex (.digest md5))))

(defn- user-avatar []
  (comp/avatar
    {:style user-avatar-style
     :src   (->> (storage/email)
                 (user-gravatar-hash)
                 (str "https://www.gravatar.com/avatar/"))}))

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
       :onTouchTap  (fn []
                      (dispatch!
                        (routes/path-for-frontend :password)))
       :primaryText "Change password"})
    (comp/menu-item
      {:key         "user-menu-logout"
       :onTouchTap  (fn []
                      (storage/remove "token")
                      (eventsource/close!)
                      (dispatch!
                        (routes/path-for-frontend :login)))
       :primaryText "Log out"})))

(rum/defc userbar < rum/static []
  [:div.user-bar
   (user-avatar)
   [:span (storage/user)]
   (user-menu)])

(rum/defc appbar < rum/static [title]
  (comp/mui
    (comp/app-bar
      {
       :title              title
       :titleStyle         appbar-title-style
       :style              appbar-style
       :iconElementRight   (userbar)
       :iconStyleRight     appbar-icon-style
       :showMenuIconButton false})))