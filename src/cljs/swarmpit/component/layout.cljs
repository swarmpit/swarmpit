(ns swarmpit.component.layout
  (:require [rum.core :as rum]
            [clojure.string :as str]
            [swarmpit.view :as view]
            [swarmpit.view-actions :as view-actions]
            [swarmpit.component.state :as state]
            [swarmpit.component.menu :as menu]
            [swarmpit.component.header :as header]))

(enable-console-print!)

(defn- page-domain
  [handler]
  (keyword (first (str/split (name handler) #"-"))))

(def single-pages
  #{:login :error :unauthorized})

(defn- page-layout?
  [handler]
  (not (contains? single-pages handler)))

(defn- document-title
  [page-title]
  (set! (-> js/document .-title)
        (str page-title " :: swarmpit")))

(rum/defc page-single < rum/static [route]
  (view/dispatch route))

(rum/defc page-layout < rum/reactive [route]
  (let [{:keys [handler]} route
        page-domain (if handler (page-domain handler))
        actions (or (get-in route [:params :origin]) route)
        page-actions (view-actions/render actions)]
    (document-title (:title page-actions))
    [:div.Swarmpit-root
     (header/appbar page-actions)
     [:nav (menu/drawer page-domain)]
     [:main.Swarmpit-context
      [:div.Swarmpit-toolbar]
      [:div.Swarmpit-route (view/dispatch route)]]]))

(rum/defc layout < rum/reactive []
  (let [{:keys [handler] :as route} (state/react state/route-cursor)]
    (if (page-layout? handler)
      (page-layout route)
      (page-single route))))

(defn mount!
  []
  (rum/mount (layout) (.getElementById js/document "layout")))
