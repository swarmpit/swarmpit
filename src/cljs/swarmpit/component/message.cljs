(ns swarmpit.component.message
  (:require [material.icon :as icon]
            [material.components :as comp]
            [swarmpit.component.state :as state]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(enable-console-print!)

(def message-cursor [:message])

(defn- show
  [text type]
  (state/set-value {:text text
                    :time (.getTime (js/Date.))
                    :type type
                    :open true} message-cursor))

(defn info
  [text]
  (show text :info))

(defn error
  [text]
  (show text :error))

(rum/defc message-content < rum/static [className text icon]
  (comp/snackbar-content
    {:aria-describedby "message-snackbar"
     :className        className
     :message          (html [:span.Swarmpit-message#message-snackbar icon text])
     :onClose          #(state/update-value [:open] false message-cursor)
     :action           (comp/icon-button
                         {:key        "close"
                          :aria-label "Close"
                          :color      "inherit"
                          :onClick    #(state/update-value [:open] false message-cursor)}
                         (icon/close {}))}))

(rum/defc info-message < rum/reactive [text opened?]
  (comp/snackbar
    {:autoHideDuration 6000
     :anchorOrigin     {:vertical   "bottom"
                        :horizontal "center"}
     :open             opened?
     :onClose          #(state/update-value [:open] false message-cursor)}
    (message-content "Swarmpit-label-green" text (icon/check-circle {:className "Swarmpit-message-icon"}))))

(rum/defc error-message < rum/reactive [text opened?]
  (comp/snackbar
    {:autoHideDuration 300000
     :anchorOrigin     {:vertical   "bottom"
                        :horizontal "center"}
     :open             opened?
     :onClose          #(state/update-value [:open] false message-cursor)}
    (message-content "Swarmpit-label-red" text (icon/error {:className "Swarmpit-message-icon"}))))

(rum/defc message < rum/reactive []
  (let [{:keys [open type text]} (state/react message-cursor)]
    (comp/mui
      (case type
        :info (info-message text open)
        :error (error-message text open)))))

(defn mount!
  []
  (rum/mount (message) (.getElementById js/document "message")))
