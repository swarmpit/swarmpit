(ns swarmpit.component.layout
  (:require [rum.core :as rum]
            [clojure.string :as str]
            [swarmpit.view :as view]
            [swarmpit.view-actions :as view-actions]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [swarmpit.component.state :as state]
            [swarmpit.component.menu :as menu]
            [swarmpit.component.header :as header]))

(enable-console-print!)

(defn- page-domain
  [handler]
  (keyword (first (str/split (name handler) #"-"))))

(def single-pages
  #{:login :error :not-found :unauthorized})

(defn- page-layout?
  [handler]
  (not (contains? single-pages handler)))

(defn- document-title
  [page-title]
  (set! (-> js/document .-title)
        (str page-title " :: swarmpit")))

(defn version-handler
  []
  (ajax/get
    (routes/path-for-backend :version)
    {:headers    {"Authorization" nil}
     :on-success (fn [{:keys [response]}]
                   (state/update-value [:version] response state/layout-cursor)
                   (state/set-value response))}))

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

(def retrieve-version
  {:init
   (fn [state]
     (when (nil? (state/get-value [:version])) (version-handler))
     state)})

(rum/defc layout < rum/reactive
                   retrieve-version []
  (let [{:keys [handler] :as route} (state/react state/route-cursor)]
    (if (page-layout? handler)
      (page-layout route)
      (page-single route))))

(defn mount!
  []
  (rum/mount (layout) (.getElementById js/document "layout")))