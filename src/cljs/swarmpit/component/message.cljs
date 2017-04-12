(ns swarmpit.component.message
  (:require [swarmpit.material :as material]
            [rum.core :as rum]))

(enable-console-print!)

(defonce state (atom {:duration 4000
                      :message  ""
                      :open     false}))

(rum/defc info < rum/reactive []
  (let [{:keys [duration message open]} (rum/react state)]
    (material/theme
      (material/snackbar
        #js {:open             open
             :message          message
             :autoHideDuration duration}))))

(defn mount!
  []
  (rum/mount (info) (.getElementById js/document "info")))
