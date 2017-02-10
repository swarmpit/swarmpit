(ns swarmpit.component.header
  (:require [swarmpit.material :as material]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc appbar < rum/static []
  (material/theme
    (material/app-bar #js{:className          "appbar"
                          :showMenuIconButton false})))