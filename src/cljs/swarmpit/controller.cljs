(ns swarmpit.controller
  (:require [ajax.core :as ajax]
            [clojure.walk :refer [keywordize-keys]]
            [swarmpit.url :refer [dispatch! query-string]]
            [cemerick.url :refer [query->map]]
            [swarmpit.storage :as storage]
            [swarmpit.component.page-login :as page-login]
            [swarmpit.component.page-401 :as page-401]
            [swarmpit.component.page-404 :as page-404]
            [swarmpit.component.page-error :as page-error]
            [swarmpit.component.service.create-config :as screatec]
            [swarmpit.component.service.create-image :as screatei]
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
            [swarmpit.component.user.list :as ulist]
            [swarmpit.component.user.info :as uinfo]
            [swarmpit.component.user.create :as ucreate]
            [swarmpit.component.registry.info :as reginfo]
            [swarmpit.component.registry.list :as reglist]
            [swarmpit.component.registry.create :as regcreate]))

(defn- fetch
  [api api-resp-fx]
  (ajax/GET api
            {:headers       {"Authorization" (storage/get "token")}
             :handler       (fn [response]
                              (let [resp (keywordize-keys response)]
                                (-> resp api-resp-fx)))
             :error-handler (fn [{:keys [status]}]
                              (if (= status 401)
                                (page-401/mount!)
                                (page-error/mount!)))}))

(defmulti dispatch (fn [location] (:handler location)))

(defmethod dispatch :index
  [_]
  (print "TO-DO"))

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

(defmethod dispatch :service-create-image
  [_]
  (fetch "/registries/sum"
         (fn [response-reg]
           (fetch "/dockerhub/users/sum"
                  (fn [response-usr]
                    (screatei/mount! response-reg response-usr))))))

(defmethod dispatch :service-create-config
  [_]
  (let [params (keywordize-keys (query->map (query-string)))]
    (screatec/mount! (:registry params)
                     (:repository params))))

(defmethod dispatch :service-edit
  [{:keys [route-params]}]
  (fetch (str "/services/" (:id route-params))
         (fn [response]
           (sedit/mount! response))))

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
  (fetch "/admin/users"
         (fn [response]
           (ulist/mount! response))))

(defmethod dispatch :user-info
  [{:keys [route-params]}]
  (fetch (str "/admin/users/" (:id route-params))
         (fn [response]
           (uinfo/mount! response))))

(defmethod dispatch :user-create
  [_]
  (ucreate/mount!))

;;; Registry controller

(defmethod dispatch :registry-list
  [_]
  (fetch "/admin/registries"
         (fn [response]
           (reglist/mount! response))))

(defmethod dispatch :registry-info
  [{:keys [route-params]}]
  (fetch (str "/admin/registries/" (:id route-params))
         (fn [response]
           (reginfo/mount! response))))

(defmethod dispatch :registry-create
  [_]
  (regcreate/mount!))