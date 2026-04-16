(ns swarmpit.app
  (:require [swarmpit.router :as router]
            [swarmpit.storage :as storage]
            [swarmpit.component.layout :as layout]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [material.components :as comp]))

;; Initialize theme from localStorage or system preference
(let [stored (storage/get "theme")
      system-dark? (and (exists? js/window.matchMedia)
                        (.-matches (.matchMedia js/window "(prefers-color-scheme: dark)")))
      theme (cond
              (= stored "dark") "dark"
              (= stored "light") "light"
              system-dark? "dark"
              :else "light")]
  (reset! comp/theme-mode theme)
  (state/update-value [:theme] theme state/layout-cursor))

;; Listen for system theme changes (only applies when no explicit preference is saved)
(when (exists? js/window.matchMedia)
  (.addEventListener
    (.matchMedia js/window "(prefers-color-scheme: dark)")
    "change"
    (fn [e]
      (when-not (storage/get "theme")
        (let [mode (if (.-matches e) "dark" "light")]
          (reset! comp/theme-mode mode)
          (state/update-value [:theme] mode state/layout-cursor))))))

;; Re-mount layout when theme changes so charts/plots re-render
(add-watch comp/theme-mode :theme-watcher
  (fn [_ _ old-mode new-mode]
    (when (not= old-mode new-mode)
      (layout/mount!))))

;; Starting router

(router/start)

;; Mounting layout

(layout/mount!)

;; Mounting message

(message/mount!)
