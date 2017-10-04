(ns swarmpit.controller
  (:refer-clojure :exclude [get])
  (:require [ajax.core :as ajax]
            [clojure.walk :refer [keywordize-keys]]
            [swarmpit.url :refer [query-string]]
            [cemerick.url :refer [query->map]]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.storage :as storage]
            [swarmpit.routes :as routes]
            [swarmpit.component.state :as state]))

(def cursor [:route])

(defmulti dispatch (fn [location] (:handler location)))

(defn- execute
  ([success-fx error-fx]
   {:headers       {"Authorization" (storage/get "token")}
    :handler       (fn [response]
                     (let [resp (keywordize-keys response)]
                       (-> resp success-fx)))
    :error-handler (fn [response]
                     (let [resp (keywordize-keys response)]
                       (-> resp error-fx)))}))

(defn get
  ([api success-fx]
   (get api success-fx (fn [{:keys [status]}]
                         (case status
                           401 (dispatch!
                                 (routes/path-for-frontend :login))
                           403 (dispatch {:handler :unauthorized})
                           404 (dispatch {:handler :not-found})
                           (dispatch {:handler :error})))))
  ([api success-fx error-fx]
   (ajax/GET api
             (execute success-fx error-fx))))

(defmethod dispatch nil
  [{:keys [handler]}]
  (state/set-value {:handler handler} cursor))

(defmethod dispatch :not-found
  [{:keys [handler]}]
  (state/set-value {:handler handler} cursor))

(defmethod dispatch :unauthorized
  [{:keys [handler]}]
  (state/set-value {:handler handler} cursor))

(defmethod dispatch :error
  [{:keys [handler]}]
  (state/set-value {:handler handler} cursor))

(defmethod dispatch :login
  [{:keys [handler]}]
  (state/set-value {:handler handler} cursor))

(defmethod dispatch :password
  [{:keys [handler]}]
  (state/set-value {:handler handler} cursor))

;;; Service controller

(defmethod dispatch :service-list
  [{:keys [handler]}]
  (get (routes/path-for-backend :services)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

(defmethod dispatch :service-info
  [{:keys [route-params handler]}]
  (get (routes/path-for-backend :service route-params)
       (fn [service]
         (get (routes/path-for-backend :service-tasks route-params)
              (fn [tasks]
                (get (routes/path-for-backend :service-networks route-params)
                     (fn [networks]
                       (state/set-value {:handler handler
                                         :data    {:service  service
                                                   :tasks    tasks
                                                   :networks networks}} cursor))))))))

(defmethod dispatch :service-log
  [{:keys [route-params handler]}]
  (get (routes/path-for-backend :service route-params)
       (fn [service]
         (state/set-value {:handler handler
                           :data    service} cursor))))

(defmethod dispatch :service-create-image
  [{:keys [handler]}]
  (get (routes/path-for-backend :registries)
       (fn [registries]
         (get (routes/path-for-backend :dockerhub-users)
              (fn [users]
                (state/set-value {:handler handler
                                  :data    {:registries registries
                                            :users      users}} cursor))))))

(defmethod dispatch :service-create-config
  [{:keys [handler]}]
  (let [params (keywordize-keys (query->map (query-string)))]
    (state/set-value {:handler handler
                      :data    params} cursor)))

(defmethod dispatch :service-edit
  [{:keys [route-params handler]}]
  (get (routes/path-for-backend :service route-params)
       (fn [service]
         (get (routes/path-for-backend :service-networks route-params)
              (fn [networks]
                (state/set-value {:handler handler
                                  :data    {:service  service
                                            :networks networks}} cursor))))))

;;; Network controller

(defmethod dispatch :network-list
  [{:keys [handler]}]
  (get (routes/path-for-backend :networks)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

(defmethod dispatch :network-info
  [{:keys [route-params handler]}]
  (get (routes/path-for-backend :network route-params)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

(defmethod dispatch :network-create
  [{:keys [handler]}]
  (state/set-value {:handler handler} cursor))

;;; Node controller

(defmethod dispatch :node-list
  [{:keys [handler]}]
  (get (routes/path-for-backend :nodes)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

;;; Volume controller

(defmethod dispatch :volume-list
  [{:keys [handler]}]
  (get (routes/path-for-backend :volumes)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

(defmethod dispatch :volume-info
  [{:keys [route-params handler]}]
  (get (routes/path-for-backend :volume route-params)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

(defmethod dispatch :volume-create
  [{:keys [handler]}]
  (state/set-value {:handler handler} cursor))

;;; Secret controller

(defmethod dispatch :secret-list
  [{:keys [handler]}]
  (get (routes/path-for-backend :secrets)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

(defmethod dispatch :secret-info
  [{:keys [route-params handler]}]
  (get (routes/path-for-backend :secret route-params)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

(defmethod dispatch :secret-create
  [{:keys [handler]}]
  (state/set-value {:handler handler} cursor))

;;; Task controller

(defmethod dispatch :task-list
  [{:keys [handler]}]
  (get (routes/path-for-backend :tasks)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

(defmethod dispatch :task-info
  [{:keys [route-params handler]}]
  (get (routes/path-for-backend :task route-params)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

;;; User controller

(defmethod dispatch :user-list
  [{:keys [handler]}]
  (get (routes/path-for-backend :users)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

(defmethod dispatch :user-info
  [{:keys [route-params handler]}]
  (get (routes/path-for-backend :user route-params)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

(defmethod dispatch :user-create
  [{:keys [handler]}]
  (state/set-value {:handler handler} cursor))

(defmethod dispatch :user-edit
  [{:keys [route-params handler]}]
  (get (routes/path-for-backend :user route-params)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

;;; Registry controller

(defmethod dispatch :registry-list
  [{:keys [handler]}]
  (get (routes/path-for-backend :registries)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

(defmethod dispatch :registry-info
  [{:keys [route-params handler]}]
  (get (routes/path-for-backend :registry route-params)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

(defmethod dispatch :registry-create
  [{:keys [handler]}]
  (state/set-value {:handler handler} cursor))

(defmethod dispatch :registry-edit
  [{:keys [route-params handler]}]
  (get (routes/path-for-backend :registry route-params)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

;;; Dockerhub user controller

(defmethod dispatch :dockerhub-user-list
  [{:keys [handler]}]
  (get (routes/path-for-backend :dockerhub-users)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

(defmethod dispatch :dockerhub-user-info
  [{:keys [route-params handler]}]
  (get (routes/path-for-backend :dockerhub-user route-params)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

(defmethod dispatch :dockerhub-user-create
  [{:keys [handler]}]
  (state/set-value {:handler handler} cursor))

(defmethod dispatch :dockerhub-user-edit
  [{:keys [route-params handler]}]
  (get (routes/path-for-backend :dockerhub-user route-params)
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))