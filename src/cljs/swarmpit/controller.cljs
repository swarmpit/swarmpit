(ns swarmpit.controller
  (:require [ajax.core :as ajax]
            [clojure.walk :refer [keywordize-keys]]
            [swarmpit.url :refer [dispatch! query-string]]
            [cemerick.url :refer [query->map]]
            [swarmpit.storage :as storage]
            [swarmpit.component.page-login :as page-login]
            [swarmpit.component.page-404 :as page-404]
            [swarmpit.component.page-error :as page-error]
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
            [swarmpit.component.task.info :as tinfo]
            [swarmpit.component.registry.list :as reglist]
            [swarmpit.component.registry.create :as regcreate]
            [swarmpit.component.registry.wizard :as regwizard]
            [swarmpit.component.repository.v1.list :as r1list]
            [swarmpit.component.repository.v2.list :as r2list]))

(defn- fetch
  [api api-resp-fx]
  (ajax/GET api
            {:headers       {"Authorization" (storage/get "token")}
             :handler       (fn [response]
                              (let [resp (keywordize-keys response)]
                                (-> resp api-resp-fx)))
             :error-handler (fn [{:keys [status]}]
                              (if (= status 401)
                                (dispatch! "/#/login")
                                (dispatch! "/#/error")))}))

(defmulti dispatch (fn [location] (:handler location)))

(defmethod dispatch :index
  [_]
  (print "index"))

(defmethod dispatch :error
  [_]
  (page-error/mount!))

(defmethod dispatch nil
  [_]
  (page-404/mount!))

(defmethod dispatch :login
  [_]
  (page-login/mount!))

;;; Service controller

(defmethod dispatch :service-list
  [_]
  (fetch "/services"
         (fn [response]
           (slist/mount! response))))

(defmethod dispatch :service-info
  [{:keys [route-params]}]
  (fetch (str "/services/" (:id route-params))
         (fn [response]
           (sinfo/mount! response))))

(defmethod dispatch :service-create
  [_]
  (let [params (keywordize-keys (query->map (query-string)))]
    (screate/mount! (:registry params)
                    (:registryVersion params)
                    (:repository params))))

(defmethod dispatch :service-edit
  [{:keys [route-params]}]
  (fetch (str "/services/" (:id route-params))
         (fn [response]
           (sedit/mount! response))))

;;; Repository controller

(defmethod dispatch :repository-v1-list
  [{:keys [route-params]}]
  (r1list/mount! (:name route-params)))

(defmethod dispatch :repository-v2-list
  [{:keys [route-params]}]
  (r2list/mount! (:name route-params)))

;;; Network controller

(defmethod dispatch :network-list
  [_]
  (fetch "/networks"
         (fn [response]
           (nlist/mount! response))))

(defmethod dispatch :network-info
  [{:keys [route-params]}]
  (fetch (str "/networks/" (:id route-params))
         (fn [response]
           (ninfo/mount! response))))

(defmethod dispatch :network-create
  [_]
  (ncreate/mount!))

;;; Network controller

(defmethod dispatch :node-list
  [_]
  (fetch "/nodes"
         (fn [response]
           (ndlist/mount! response))))

(defmethod dispatch :node-info
  [{:keys [route-params]}]
  (fetch (str "/nodes/" (:id route-params))
         (fn [response]
           (ndinfo/mount! response))))

;;; Task controller

(defmethod dispatch :task-list
  [_]
  (fetch "/tasks"
         (fn [response]
           (tlist/mount! response))))

(defmethod dispatch :task-info
  [{:keys [route-params]}]
  (fetch (str "/tasks/" (:id route-params))
         (fn [response]
           (tinfo/mount! response))))

;;; User controller

(defmethod dispatch :user-list
  [_]
  (fetch "/users"
         (fn [response]
           (tlist/mount! response))))

;;; Registry controller

(defmethod dispatch :registry-list
  [_]
  (fetch "/registries"
         (fn [response]
           (reglist/mount! response))))

(defmethod dispatch :registry-create
  [_]
  (regcreate/mount!))

(defmethod dispatch :registry-wizard
  [_]
  (fetch "/registries/sum"
         (fn [response]
           (regwizard/mount! response))))