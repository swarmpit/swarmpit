(ns swarmpit.component.header
  (:require [swarmpit.material :as material :refer [svg]]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc appbar < rum/static []
  (material/theme
    (material/app-bar #js{:title              "Services"
                          :titleStyle         #js {:fontSize   "20px"
                                                   :fontWeight 200}
                          ;:iconElementLeft    (material/icon-button nil (svg material/services-icon))
                          ;:iconStyleLeft      #js {:paddingLeft "5px"}
                          :className          "appbar"
                          :showMenuIconButton false})))