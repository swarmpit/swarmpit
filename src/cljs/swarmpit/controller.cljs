(ns swarmpit.controller
  (:require [bidi.router :as br]
            [clojure.walk :as walk]
            [clojure.string :as str]
            [ajax.core :refer [GET POST]]
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

(defonce domain (atom nil))

(defn- select-domain
  [location]
  (let [route (name (:handler location))
        route-domain (first (str/split route #"-"))]
    (case route-domain
      "service" "Services"
      "task" "Tasks"
      "network" "Networks"
      "node" "Nodes"
      "Home")))

;;; Routing handler config

(def handler ["" {"/"         :index
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

(defn start
  []
  (let [router (br/start-router! handler
                                 {:on-navigate
                                  (fn [loc]
                                    (dispatch loc)
                                    (reset! location loc)
                                    (reset! domain (select-domain loc)))})
        route (:handler @location)]
    (if (some? route)
      (br/set-location! router @location))))

;;; Default controller

(defmethod dispatch :index
  [_]
  (print "index"))

(defmethod dispatch nil
  [_]
  (print "not-found"))

;;; Service controller

(defmethod dispatch :service-list
  [_]
  (GET "/services"
       {:handler (fn [response]
                   (let [res (walk/keywordize-keys response)]
                     (slist/mount! res)))}))

(defmethod dispatch :service-info
  [{:keys [route-params]}]
  (GET (str "/services/" (:id route-params))
       {:handler (fn [response]
                   (let [res (walk/keywordize-keys response)]
                     (sinfo/mount! res)))}))

(defmethod dispatch :service-create
  [_]
  (screate/mount!))

(defmethod dispatch :service-edit
  [{:keys [route-params]}]
  (print "edit")
  (GET (str "/services/" (:id route-params))
       {:handler (fn [response]
                   (let [res (walk/keywordize-keys response)]
                     (sedit/mount! res)))}))

;;; Network controller

(defmethod dispatch :network-list
  [_]
  (GET "/networks"
       {:handler (fn [response]
                   (let [res (walk/keywordize-keys response)]
                     (nlist/mount! res)))}))

(defmethod dispatch :network-info
  [{:keys [route-params]}]
  (GET (str "/networks/" (:id route-params))
       {:handler (fn [response]
                   (let [res (walk/keywordize-keys response)]
                     (ninfo/mount! res)))}))

(defmethod dispatch :network-create
  [_]
  (ncreate/mount!))

;;; Network controller

(defmethod dispatch :node-list
  [_]
  (GET "/nodes"
       {:handler (fn [response]
                   (let [res (walk/keywordize-keys response)]
                     (ndlist/mount! res)))}))

(defmethod dispatch :node-info
  [{:keys [route-params]}]
  (GET (str "/nodes/" (:id route-params))
       {:handler (fn [response]
                   (let [res (walk/keywordize-keys response)]
                     (ndinfo/mount! res)))}))

;;; Task controller

(defmethod dispatch :task-list
  [_]
  (GET "/tasks"
       {:handler (fn [response]
                   (let [res (walk/keywordize-keys response)]
                     (tlist/mount! res)))}))

(defmethod dispatch :task-info
  [{:keys [route-params]}]
  (GET (str "/tasks/" (:id route-params))
       {:handler (fn [response]
                   (let [res (walk/keywordize-keys response)]
                     (tinfo/mount! res)))}))
