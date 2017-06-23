(ns swarmpit.component.layout
  (:require [rum.core :as rum]
            [swarmpit.controller :as controller]
            [swarmpit.view :as view]
            [swarmpit.component.state :as state]
            [swarmpit.component.menu :as menu]
            [swarmpit.component.header :as header]))

(enable-console-print!)

(def page-titles
  {:index                 "Home"
   :password              "Change password"
   :service-list          "Services"
   :service-create-config "Services / Wizard"
   :service-create-image  "Services / Wizard"
   :service-info          "Services"
   :service-edit          "Services"
   :network-list          "Networks"
   :network-create        "Networks / Create"
   :network-info          "Networks"
   :volume-list           "Volumes"
   :volume-create         "Volumes / Create"
   :volume-info           "Volumes"
   :secret-list           "Secrets"
   :secret-create         "Secrets / Create"
   :secret-info           "Secrets"
   :node-list             "Nodes"
   :task-list             "Tasks"
   :task-info             "Tasks"
   :user-list             "Users / Swarmpit"
   :user-create           "Users / Swarmpit / Create"
   :user-info             "Users / Swarmpit "
   :registry-info         "Registries"
   :registry-list         "Registries"
   :registry-create       "Registries / Add"
   :dockerhub-user-info   "Users / Dockerhub"
   :dockerhub-user-list   "Users / Dockerhub"
   :dockerhub-user-create "Users / Dockerhub / Add"})

(defn- page-title
  [handler]
  (get page-titles handler))

(def single-pages
  #{:login :error :unauthorized nil})

(defn- page-layout?
  [handler]
  (not (contains? single-pages handler)))

(rum/defc page-single < rum/static [route]
  (view/dispatch route))

(rum/defc page-layout < rum/reactive [route]
  (let [{:keys [opened]} (state/react menu/cursor)
        {:keys [handler]} route
        layout-type (if opened
                      "layout-opened"
                      "layout-closed")
        layout-title (page-title handler)]
    [:div {:class ["layout" layout-type]}
     [:header (header/appbar layout-title)]
     [:nav (menu/drawer layout-title)]
     [:main (view/dispatch route)]]))

(rum/defc layout < rum/reactive []
  (let [{:keys [handler] :as route} (state/react controller/cursor)]
    (when (some? route)
      (if (page-layout? handler)
        (page-layout route)
        (page-single route)))))

(defn mount!
  []
  (rum/mount (layout) (.getElementById js/document "layout")))