(ns swarmpit.component.layout
  (:require [rum.core :as rum]
            [material.components :as comp]
            [clojure.string :as str]
            [swarmpit.view :as view]
            [swarmpit.view-actions :as view-actions]
            [swarmpit.component.state :as state]
            [swarmpit.component.menu :as menu]
            [swarmpit.component.header :as header]))

(enable-console-print!)

(def styles
  (let [theme (js->clj comp/theme)]
    {:root    {:flexGrow 1
               :zIndex   1
               :overflow "hidden"
               :position "relative"
               :display  "flex"}
     :context {:flexGrow        1
               :backgroundColor (-> theme (get-in ["palette" "background" "default"]))
               :padding         (* (-> theme (get-in ["spacing" "unit"])) 3)}}))

(defn- page-domain
  [handler]
  (keyword (first (str/split (name handler) #"-"))))

(def single-pages
  #{:login :error :unauthorized :not-found nil})

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
        page-domain (page-domain handler)
        page-actions (view-actions/render route)]
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
