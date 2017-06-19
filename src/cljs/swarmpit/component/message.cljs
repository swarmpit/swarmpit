(ns swarmpit.component.message
  (:require [material.component :as comp]
            [rum.core :as rum]))

(enable-console-print!)

(def message-body-style
  {:backgroundColor "#fff"
   :maxWidth        "100%"
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

(def message-style
  {:maxWidth "100%"
   :minWidth "50px"
   :height   "auto"})

(defn- message
  [props]
  (comp/mui
    (comp/snackbar
      (merge props
             {:open  true
              :style message-style}))))

(rum/defc info-message < rum/reactive [text]
  (message {:bodyStyle        message-info-body-style
            :contentStyle     message-info-content-style
            :autoHideDuration 5000
            :message          text}))

(rum/defc error-message < rum/reactive [text]
  (message {:bodyStyle        message-error-body-style
            :contentStyle     message-error-content-style
            :autoHideDuration 300000
            :message          text}))

(defn mount!
  ([text] (mount! text false))
  ([text isError?]
   (if isError?
     (rum/mount (error-message text) (.getElementById js/document "message"))
     (rum/mount (info-message text) (.getElementById js/document "message")))))
