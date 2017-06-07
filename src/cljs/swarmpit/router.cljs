(ns swarmpit.router
  (:require [bidi.router :as br]
            [swarmpit.storage :as storage]
            [swarmpit.controller :as ctrl]
            [swarmpit.component.state :as state]
            [swarmpit.component.layout :as layout]))

(defonce location (atom nil))

(def location-domains
  {:index                 "Home"
   :service-list          "Services"
   :service-create-config "Services / Wizard"
   :service-create-image  "Services / Wizard"
   :service-info          "Services"
   :service-edit          "Services"
   :network-list          "Networks"
   :network-create        "Networks / Create"
   :network-info          "Networks"
   :node-list             "Nodes"
   :node-info             "Nodes"
   :task-list             "Tasks"
   :task-info             "Tasks"
   :user-list             "Users"
   :user-create           "Users / Create"
   :user-info             "Users"
   :registry-info         "Registries"
   :registry-list         "Registries"
   :registry-create       "Registries / Create"})

(def location-page
  #{:login nil})

(def routes ["" {"/"           :index
                 "/login"      :login
                 "/error"      :error
                 "/services"   {""                :service-list
                                "/create/wizard"  {"/image"  :service-create-image
                                                   "/config" :service-create-config}
                                ["/" :id]         :service-info
                                ["/" :id "/edit"] :service-edit}
                 "/networks"   {""        :network-list
                                "/create" :network-create
                                ["/" :id] :network-info}
                 "/nodes"      {""        :node-list
                                ["/" :id] :node-info}
                 "/tasks"      {""        :task-list
                                ["/" :id] :task-info}
                 "/registries" {""        :registry-list
                                "/create" :registry-create
                                ["/" :id] :registry-info}
                 "/users"      {""        :user-list
                                "/create" :user-create
                                ["/" :id] :user-info}}])

;;; Router config

(defn- is-layout?
  "Check whether `loc` belong to layout. Not single page!"
  [loc]
  (not (contains? location-page loc)))

(defn- route
  "Route to given `loc`"
  [loc]
  (ctrl/dispatch loc)
  (reset! location loc))

(defn- route-to-loc
  "Route to given `loc` and update state domain"
  [loc]
  (let [domain (get location-domains (:handler loc))]
    (state/update-value [:domain] domain [:menu])
    (if (is-layout? loc)
      (layout/mount!))
    (route loc)))

(defn- route-to-login
  "Route to login page"
  []
  (let [login {:handler :login}]
    (route login)))

(defn- navigate
  [loc]
  (if (nil? (storage/get "token"))
    (route-to-login)
    (route-to-loc loc)))

(defn start
  []
  (let [router (br/start-router! routes {:on-navigate navigate})
        route (:handler @location)]
    (if (some? route)
      (br/set-location! router @location))))
