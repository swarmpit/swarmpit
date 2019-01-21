(ns swarmpit.component.dialog
  (:require [material.icon :as icon]
            [material.components :as comp]
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

(rum/defc form-dialog < rum/reactive [action-fn form dialog-title]
  (let [{:keys [open]} (state/react dialog-cursor)]
    (comp/responsive-dialog
      {:disableBackdropClick true
       :disableEscapeKeyDown true
       :breakpoint           "xs"
       :open                 open
       :aria-labelledby      "form-dialog-title"}
      (comp/dialog-title
        {:id "form-dialog-title"} dialog-title)
      (comp/dialog-content
        {} form)
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

(rum/defc form-full-dialog < rum/reactive [action-fn action-title form dialog-title]
  (let [{:keys [open]} (state/react dialog-cursor)]
    (comp/mui
      (comp/dialog
        {:fullScreen      true
         :open            open
         :aria-labelledby "form-full-dialog-title"}
        (comp/app-bar
          {:color "primary"
           :style {:position "relative"}}
          (comp/toolbar
            {:disableGutters false}
            (comp/icon-button
              {:color   "inherit"
               :onClick #(state/update-value [:open] false dialog-cursor)}
              (icon/close {}))
            (comp/typography
              {:key       "appbar-title"
               :variant   "h6"
               :color     "inherit"
               :className "grow"
               :noWrap    true}
              dialog-title)
            (comp/button
              {:color   "inherit"
               :onClick action-fn}
              action-title))) form))))