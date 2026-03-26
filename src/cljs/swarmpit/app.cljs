(ns swarmpit.app
  (:require [swarmpit.router :as router]
            [swarmpit.storage :as storage]
            [swarmpit.component.state :as state]
            [swarmpit.component.layout :as layout]
            [swarmpit.component.message :as message]
            [material.components :as comp]))

;; Initialize theme from localStorage or system preference
(let [saved (storage/get-theme)
      system-dark (and (exists? js/window.matchMedia)
                       (.-matches (.matchMedia js/window "(prefers-color-scheme: dark)")))
      mode (or saved (if system-dark "dark" "light"))]
  (reset! comp/theme-mode mode)
  (state/set-value mode [:theme]))

;; Re-mount layout when theme changes
(add-watch comp/theme-mode :theme-change
  (fn [_ _ old-mode new-mode]
    (when (not= old-mode new-mode)
      (layout/mount!))))

;; Follow system theme changes when user hasn't explicitly chosen
(when (exists? js/window.matchMedia)
  (let [mq (.matchMedia js/window "(prefers-color-scheme: dark)")]
    (.addEventListener mq "change"
      (fn [e]
        (when-not (storage/get-theme)
          (let [mode (if (.-matches e) "dark" "light")]
            (reset! comp/theme-mode mode)
            (state/set-value mode [:theme])))))))

;; Starting router
(router/start)

;; Mounting layout
(layout/mount!)

;; Mounting message
(message/mount!)
