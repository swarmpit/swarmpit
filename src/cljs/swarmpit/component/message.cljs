(ns swarmpit.component.message
  (:require [material.component :as comp]
            [rum.core :as rum]))

(enable-console-print!)

(rum/defc message < rum/static [text]
  (comp/mui
    (comp/snackbar
      {:open             true
       :message          text
       :autoHideDuration 4000})))

(defn mount!
  [text]
  (rum/mount (message text) (.getElementById js/document "message")))
