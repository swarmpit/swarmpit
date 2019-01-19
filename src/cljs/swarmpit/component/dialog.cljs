(ns swarmpit.component.dialog
  (:require [material.components :as comp]
            [swarmpit.component.state :as state]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def dialog-cursor [:dialog])

(rum/defc confirm-dialog < rum/reactive [action-fn action-text dialog-title]
  (let [{:keys [open]} (state/react dialog-cursor)]
    (comp/dialog
      {:disableBackdropClick true
       :disableEscapeKeyDown true
       :maxWidth             "xs"
       :open                 open
       :onEntering           #()
       :aria-labelledby      "confirmation-dialog-title"}
      (comp/dialog-title
        {:id "confirmation-dialog-title"} dialog-title)
      (comp/dialog-content
        {}
        (html [:span action-text]))
      (comp/dialog-actions
        {}
        (comp/button
          {:onClick (fn []
                      (action-fn)
                      (state/update-value [:open] false dialog-cursor))
           :color   "primary"} "Ok")
        (comp/button
          {:onClick #(state/update-value [:open] false dialog-cursor)
           :color   "primary"} "Cancel")))))
