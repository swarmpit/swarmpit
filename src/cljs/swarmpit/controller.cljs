(ns swarmpit.controller
  (:require [bidi.router :as br]
            [clojure.walk :as walk]
            [clojure.string :as str]
            [ajax.core :as ajax]
            [swarmpit.router :as router]
            [swarmpit.storage :as storage]
            [swarmpit.component.header :as header]
            [swarmpit.component.menu :as menu]
            [swarmpit.component.layout :as layout]
            [swarmpit.component.user.login :as ulogin]
            [swarmpit.component.service.create :as screate]
            [swarmpit.component.service.edit :as sedit]
            [swarmpit.component.service.info :as sinfo]
            [swarmpit.component.service.list :as slist]
            [swarmpit.component.network.create :as ncreate]
            [swarmpit.component.network.info :as ninfo]
            [swarmpit.component.network.list :as nlist]
            [swarmpit.component.node.list :as ndlist]
            [swarmpit.component.node.info :as ndinfo]
            [swarmpit.component.task.list :as tlist]
            [swarmpit.component.task.info :as tinfo]))

(defmulti dispatch (fn [location] (:handler location)))

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
  (dispatch loc)
  (reset! location loc))

(defn- route-to-loc
  "Route to given `loc` and setup domain"
  [loc]
  (let [domain (get resource loc)]
    (route loc)
    (swap! menu/state assoc :domain domain)
    (swap! header/state assoc :domain domain)))

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

;;;

(defn GET
  [api resp-fx]
  (ajax/GET api
            {:headers       {"Authorization" (storage/get "token")}
             :handler       (fn [response]
                              (let [resp (walk/keywordize-keys response)]
                                (layout/mount!)
                                (-> resp resp-fx)))
             :error-handler (fn [{:keys [status]}]
                              (if (= status 401)
                                (router/dispatch! "/#/login")
                                (router/dispatch! "/#/login")))}))

;;; Default controller

(defmethod dispatch :index
  [_]
  (print "index"))

(defmethod dispatch :error
  [_]
  (print "ups something went wrong"))

(defmethod dispatch nil
  [_]
  (print "not-found"))

(defmethod dispatch :login
  [_]
  (ulogin/mount!))

;;; Service controller

(defmethod dispatch :service-list
  [_]
  (GET "/services"
       (fn [response]
         (slist/mount! response))))

(defmethod dispatch :service-info
  [{:keys [route-params]}]
  (GET (str "/services/" (:id route-params))
       (fn [response]
         (sinfo/mount! response))))

(defmethod dispatch :service-create
  [_]
  (screate/mount!))

(defmethod dispatch :service-edit
  [{:keys [route-params]}]
  (GET (str "/services/" (:id route-params))
       (fn [response]
         (sedit/mount! response))))

;;; Network controller

(defmethod dispatch :network-list
  [_]
  (GET "/networks"
       (fn [response]
         (nlist/mount! response))))

(defmethod dispatch :network-info
  [{:keys [route-params]}]
  (GET (str "/networks/" (:id route-params))
       (fn [response]
         (ninfo/mount! response))))

(defmethod dispatch :network-create
  [_]
  (ncreate/mount!))

;;; Network controller

(defmethod dispatch :node-list
  [_]
  (GET "/nodes"
       (fn [response]
         (ndlist/mount! response))))

(defmethod dispatch :node-info
  [{:keys [route-params]}]
  (GET (str "/nodes/" (:id route-params))
       (fn [response]
         (ndinfo/mount! response))))

;;; Task controller

(defmethod dispatch :task-list
  [_]
  (GET "/tasks"
       (fn [response]
         (tlist/mount! response))))

(defmethod dispatch :task-info
  [{:keys [route-params]}]
  (GET (str "/tasks/" (:id route-params))
       (fn [response]
         (tinfo/mount! response))))