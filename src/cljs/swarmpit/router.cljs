(ns swarmpit.router
  (:require [bidi.router :as br]
            [swarmpit.storage :as storage]
            [swarmpit.controller :as ctrl]
            [swarmpit.component.state :as state]
            [swarmpit.component.layout :as layout]))

(defonce location (atom nil))

(def resource
  {:index          "Home"
   :login          ""
   :service-list   "Services"
   :service-create "Services"
   :service-info   "Services"
   :service-edit   "Services"
   :network-list   "Networks"
   :network-create "Networks"
   :network-info   "Networks"
   :node-list      "Nodes"
   :node-info      "Nodes"
   :task-list      "Tasks"
   :task-info      "Tasks"})

(def handler ["" {"/"         :index
                  "/login"    :login
                  "/services" {""                :service-list
                               "/create"         :service-create
                               ["/" :id]         :service-info
                               ["/" :id "/edit"] :service-edit}
                  "/networks" {""        :network-list
                               "/create" :network-create
                               ["/" :id] :network-info}
                  "/nodes"    {""        :node-list
                               ["/" :id] :node-info}
                  "/tasks"    {""        :task-list
                               ["/" :id] :task-info}}])

;;; Router config

(defn- route
  "Route to given `loc`"
  [loc]
  (ctrl/dispatch loc)
  (reset! location loc))

(defn- route-to-loc
  "Route to given `loc` and update state domain"
  [loc]
  (let [domain (get resource (:handler loc))]
    (state/update-value :domain domain [:menu])
    (if (some? domain)
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
  (let [router (br/start-router! handler {:on-navigate navigate})
        route (:handler @location)]
    (if (some? route)
      (br/set-location! router @location))))