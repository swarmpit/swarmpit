(ns swarmpit.controller
  (:require [ajax.core :as ajax]
            [clojure.walk :refer [keywordize-keys]]
            [swarmpit.url :refer [dispatch! query-string]]
            [cemerick.url :refer [query->map]]
            [swarmpit.storage :as storage]
            [swarmpit.routes :as routes]
            [swarmpit.component.page-login :as page-login]
            [swarmpit.component.page-401 :as page-401]
            [swarmpit.component.page-404 :as page-404]
            [swarmpit.component.page-error :as page-error]
            [swarmpit.component.password :as passwd]
            [swarmpit.component.service.create-config :as screatec]
            [swarmpit.component.service.create-image :as screatei]
            [swarmpit.component.service.edit :as sedit]
            [swarmpit.component.service.info :as sinfo]
            [swarmpit.component.service.list :as slist]
            [swarmpit.component.network.create :as ncreate]
            [swarmpit.component.network.info :as ninfo]
            [swarmpit.component.network.list :as nlist]
            [swarmpit.component.volume.create :as vcreate]
            [swarmpit.component.volume.info :as vinfo]
            [swarmpit.component.volume.list :as vlist]
            [swarmpit.component.node.list :as ndlist]
            [swarmpit.component.node.info :as ndinfo]
            [swarmpit.component.task.list :as tlist]
            [swarmpit.component.task.info :as tinfo]
            [swarmpit.component.user.list :as ulist]
            [swarmpit.component.user.info :as uinfo]
            [swarmpit.component.user.create :as ucreate]
            [swarmpit.component.registry.info :as reginfo]
            [swarmpit.component.registry.list :as reglist]
            [swarmpit.component.registry.create :as regcreate]
            [swarmpit.component.dockerhub.info :as dhinfo]
            [swarmpit.component.dockerhub.list :as dhlist]
            [swarmpit.component.dockerhub.create :as dhcreate]))

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

(defmethod dispatch :password
  [_]
  (passwd/mount!))

;;; Service controller

(defmethod dispatch :service-list
  [_]
  (fetch (routes/path-for-backend :services)
         (fn [response]
           (slist/mount! response))))

(defmethod dispatch :service-info
  [{:keys [route-params]}]
  (fetch (routes/path-for-backend :service route-params)
         (fn [response]
           (sinfo/mount! response))))

(defmethod dispatch :service-create-image
  [_]
  (fetch (routes/path-for-backend :registries-sum)
         (fn [registries]
           (fetch (routes/path-for-backend :dockerhub-users-sum)
                  (fn [users]
                    (screatei/mount! registries users))))))

(defmethod dispatch :service-create-config
  [_]
  (let [params (keywordize-keys (query->map (query-string)))]
    (fetch (routes/path-for-backend :networks)
           (fn [networks]
             (screatec/mount! (:registry params)
                              (:repository params)
                              networks)))))

(defmethod dispatch :service-edit
  [{:keys [route-params]}]
  (fetch (routes/path-for-backend :service-update route-params)
         (fn [response]
           (sedit/mount! response))))

;;; Network controller

(defmethod dispatch :network-list
  [_]
  (fetch (routes/path-for-backend :networks)
         (fn [response]
           (nlist/mount! response))))

(defmethod dispatch :network-info
  [{:keys [route-params]}]
  (fetch (routes/path-for-backend :network route-params)
         (fn [response]
           (ninfo/mount! response))))

(defmethod dispatch :network-create
  [_]
  (ncreate/mount!))

;;; Network controller

(defmethod dispatch :node-list
  [_]
  (fetch (routes/path-for-backend :nodes)
         (fn [response]
           (ndlist/mount! response))))

(defmethod dispatch :node-info
  [{:keys [route-params]}]
  (fetch (routes/path-for-backend :node route-params)
         (fn [response]
           (ndinfo/mount! response))))

;;; Volume controller

(defmethod dispatch :volume-list
  [_]
  (fetch (routes/path-for-backend :volumes)
         (fn [response]
           (vlist/mount! response))))

(defmethod dispatch :volume-info
  [{:keys [route-params]}]
  (fetch (routes/path-for-backend :volume route-params)
         (fn [response]
           (vinfo/mount! response))))

(defmethod dispatch :volume-create
  [_]
  (vcreate/mount!))

;;; Task controller

(defmethod dispatch :task-list
  [_]
  (fetch (routes/path-for-backend :tasks)
         (fn [response]
           (tlist/mount! response))))

(defmethod dispatch :task-info
  [{:keys [route-params]}]
  (fetch (routes/path-for-backend :task route-params)
         (fn [response]
           (tinfo/mount! response))))

;;; User controller

(defmethod dispatch :user-list
  [_]
  (fetch (routes/path-for-backend :users)
         (fn [response]
           (ulist/mount! response))))

(defmethod dispatch :user-info
  [{:keys [route-params]}]
  (fetch (routes/path-for-backend :user route-params)
         (fn [response]
           (uinfo/mount! response))))

(defmethod dispatch :user-create
  [_]
  (ucreate/mount!))

;;; Registry controller

(defmethod dispatch :registry-list
  [_]
  (fetch (routes/path-for-backend :registries)
         (fn [response]
           (reglist/mount! response))))

(defmethod dispatch :registry-info
  [{:keys [route-params]}]
  (fetch (routes/path-for-backend :registry route-params)
         (fn [response]
           (reginfo/mount! response))))

(defmethod dispatch :registry-create
  [_]
  (regcreate/mount!))

;;; Dockerhub user controller

(defmethod dispatch :dockerhub-user-list
  [_]
  (fetch (routes/path-for-backend :dockerhub-users)
         (fn [response]
           (dhlist/mount! response))))

(defmethod dispatch :dockerhub-user-info
  [{:keys [route-params]}]
  (fetch (routes/path-for-backend :dockerhub-user route-params)
         (fn [response]
           (dhinfo/mount! response))))

(defmethod dispatch :dockerhub-user-create
  [_]
  (dhcreate/mount!))

