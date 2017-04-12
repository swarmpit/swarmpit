(ns swarmpit.component.message
  (:require [swarmpit.material :as material]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc message < rum/static [text]
  (material/theme
    (material/snackbar
      #js {:open             true
           :message          text
           :autoHideDuration 4000})))

(defn mount!
  [text]
  (rum/mount (message text) (.getElementById js/document "message")))
