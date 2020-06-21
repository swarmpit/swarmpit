(ns swarmpit.component.dialog
  (:require [material.icon :as icon]
            [material.components :as comp]
            [swarmpit.component.state :as state]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def dialog-cursor [:dialog])

(rum/defc confirm-dialog < rum/reactive [action-fn action-text ok-text & dialog-title]
  (let [{:keys [open]} (state/react dialog-cursor)]
    (comp/dialog
      {:disableBackdropClick true
       :disableEscapeKeyDown true
       :maxWidth             "xs"
       :fullWidth            true
       :open                 open
       :onEntering           #()
       :aria-labelledby      "confirmation-dialog-title"}
      (when dialog-title
        (comp/dialog-title
          {:id "confirmation-dialog-title"} dialog-title))
      (comp/dialog-content
        {}
        action-text)
      (comp/dialog-actions
        {}
        (comp/button
          {:onClick #(state/update-value [:open] false dialog-cursor)
           :color   "primary"} "Cancel")
        (comp/button
          {:onClick (fn []
                      (action-fn)
                      (state/update-value [:open] false dialog-cursor))
           :color   "primary"} (or ok-text "OK"))))))