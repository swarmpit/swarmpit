(ns swarmpit.component.user.menu
  (:require [swarmpit.material :as material :refer [svg]]
            [rum.core :as rum]))

(enable-console-print!)

(defn avatar []
  (material/avatar #js{:className "user-avatar"
                       :src       "https://www.gravatar.com/avatar/6e6fd910c0594f4f2b448e3530eb5abd"}))

(defn menu []
  (material/icon-menu #js{:iconButtonElement
                          (material/icon-button
                            nil
                            (svg #js {:color "#fff"} material/expand-icon))}
                      (material/menu-item
                        #js {:primaryText "Settings"})
                      (material/menu-item
                        #js {:primaryText "Log out"})))

(rum/defc bar < rum/static []
  [:div.user-bar
   (avatar)
   [:span "admin"]
   (menu)])
