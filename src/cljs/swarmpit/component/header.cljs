(ns swarmpit.component.header
  (:require [swarmpit.material :as material :refer [svg]]
            [swarmpit.controller :as ctrl]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc appbar < rum/reactive []
  (let [domain (rum/react ctrl/domain)]
    (material/theme
      (material/app-bar #js{:title              domain
                            :titleStyle         #js {:fontSize   "20px"
                                                     :fontWeight 200}
                            ;:iconElementLeft    (material/icon-button nil (svg material/services-icon))
                            ;:iconStyleLeft      #js {:paddingLeft "5px"}
                            :className          "appbar"
                            :showMenuIconButton false}))))