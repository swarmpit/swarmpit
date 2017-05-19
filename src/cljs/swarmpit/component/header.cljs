(ns swarmpit.component.header
  (:require [swarmpit.material :as material :refer [svg]]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:menu])

(defn- avatar []
  (material/avatar #js{:className "user-avatar"
                       :src       "https://www.gravatar.com/avatar/6e6fd910c0594f4f2b448e3530eb5abd"}))

(defn- menu []
  (material/icon-menu #js{:iconButtonElement
                          (material/icon-button
                            nil
                            (svg #js {:color "#fff"} material/expand-icon))}
                      (material/menu-item
                        #js {:primaryText "Settings"})
                      (material/menu-item
                        #js {:primaryText "Log out"})))

(rum/defc userbar < rum/static []
  [:div.user-bar
   (avatar)
   [:span "admin"]
   (menu)])

(rum/defc appbar < rum/reactive []
  (let [{:keys [domain]} (state/react cursor)]
    (material/theme
      (material/app-bar #js{:title              domain
                            :titleStyle         #js {:fontSize   "20px"
                                                     :fontWeight 200}
                            :style              #js {:position "fixed"
                                                     :top      0}
                            ;:iconElementLeft    (material/icon-button nil (svg material/services-icon))
                            ;:iconStyleLeft      #js {:paddingLeft "5px"}
                            :iconElementRight   (userbar)
                            :iconStyleRight     #js {:position "fixed"
                                                     :right    20}
                            ;:className          "appbar"
                            :showMenuIconButton false}))))