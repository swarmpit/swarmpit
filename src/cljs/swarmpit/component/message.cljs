(ns swarmpit.component.message
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [rum.core :as rum]))

(enable-console-print!)

(def message-cursor [:message])

(def message-body-style
  {:backgroundColor "#fff"
   :maxWidth        "100%"
   :height          "100%"
   :lineHeight      "24px"
   :whiteSpace      "pre-line"
   :minWidth        0})

(def message-info-body-style
  (merge message-body-style
         {:border "2px solid rgb(117, 117, 117)"}))

(def message-info-content-style
  {:color "rgb(117, 117, 117)"})

(def message-error-body-style
  (merge message-body-style
         {:border "2px solid rgb(244, 67, 54)"}))

(def message-error-content-style
  {:color "rgb(244, 67, 54)"})

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

(rum/defc info-message < rum/reactive [text opened?]
  (comp/mui
    (comp/snackbar {:bodyStyle        message-info-body-style
                    :contentStyle     message-info-content-style
                    :autoHideDuration 5000
                    :message          text
                    :open             opened?})))

(rum/defc error-message < rum/reactive [text opened?]
  (comp/mui
    (comp/snackbar {:bodyStyle        message-error-body-style
                    :contentStyle     message-error-content-style
                    :autoHideDuration 300000
                    :message          text
                    :open             opened?})))

(rum/defc message < rum/reactive []
  (let [{:keys [open type text]} (state/react message-cursor)]
    (case type
      :info (info-message text open)
      :error (error-message text open))))

(defn mount!
  []
  (rum/mount (message) (.getElementById js/document "message")))
