(ns swarmpit.component.header
  (:require [swarmpit.material :as material :refer [svg]]
            [swarmpit.component.state :as state]
            [swarmpit.component.user.menu :as user]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:menu :domain])

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
                            :iconElementRight   (user/bar)
                            :iconStyleRight     #js {:position "fixed"
                                                     :right    20}
                            ;:className          "appbar"
                            :showMenuIconButton false}))))