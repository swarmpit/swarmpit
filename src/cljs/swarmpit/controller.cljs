(ns swarmpit.controller
  (:refer-clojure :exclude [get])
  (:require [ajax.core :as ajax]
            [clojure.walk :refer [keywordize-keys]]
            [swarmpit.url :refer [dispatch! query-string]]
            [cemerick.url :refer [query->map]]
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
                         (if (= status 401)
                           (dispatch {:handler :unauthorized})
                           (dispatch {:handler :error})))))
  ([api success-fx error-fx]
   (ajax/GET api
             (execute success-fx error-fx))))

(defmethod dispatch :index
  [{:keys [handler]}]
  (state/set-value {:handler handler} cursor))

(defmethod dispatch nil
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
       (fn [response]
         (state/set-value {:handler handler
                           :data    response} cursor))))

(defmethod dispatch :service-create-image
  [{:keys [handler]}]
  (get (routes/path-for-backend :registries-sum)
       (fn [registries]
         (get (routes/path-for-backend :dockerhub-users-sum)
              (fn [users]
                (state/set-value {:handler handler
                                  :data    {:registries registries
                                            :users      users}} cursor))))))

(defmethod dispatch :service-create-config
  [{:keys [handler]}]
  (let [params (keywordize-keys (query->map (query-string)))]
    (get (routes/path-for-backend :networks)
         (fn [networks]
           (get (routes/path-for-backend :volumes)
                (fn [volumes]
                  (get (routes/path-for-backend :secrets)
                       (fn [secrets]
                         (state/set-value {:handler handler
                                           :data    (assoc params
                                                      :networks networks
                                                      :volumes volumes
                                                      :secrets secrets)} cursor)))))))))

(defmethod dispatch :service-edit
  [{:keys [route-params handler]}]
  (get (routes/path-for-backend :service route-params)
       (fn [service]
         (get (routes/path-for-backend :volumes)
              (fn [volumes]
                (get (routes/path-for-backend :secrets)
                     (fn [secrets]
                       (state/set-value {:handler handler
                                         :data    {:service service
                                                   :volumes volumes
                                                   :secrets secrets}} cursor))))))))

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