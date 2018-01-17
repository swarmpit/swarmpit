(ns swarmpit.docker.client
  (:require [ring.util.codec :refer [form-encode]]
            [cheshire.core :refer [generate-string]]
            [swarmpit.base64 :as base64]
            [swarmpit.docker.http :refer :all]))

(defn- registry-token
  [auth]
  (base64/encode (generate-string auth)))

;; Service

(defn services
  []
  (-> (execute {:method :GET
                :api    "/services"})
      :body))

(defn service
  [id]
  (-> (execute {:method :GET
                :api    (str "/services/" id)})
      :body))

(defn service-tasks
  [id]
  (-> (execute {:method  :GET
                :api     "/tasks"
                :options {:query-params {:filters (generate-string {:service [id]})}}})
      :body))

(defn service-logs
  [id opt]
  (-> (execute {:method  :GET
                :api     (str "/services/" id "/logs")
                :options opt})
      :body))

(defn delete-service
  [id]
  (-> (execute {:method :DELETE
                :api    (str "/services/" id)})
      :body))

(defn create-service
  ([service]
   (-> (execute {:method  :POST
                 :api     "/services/create"
                 :options {:body    service
                           :headers {:Content-Type "application/json"}}})
       :body))
  ([auth-config service]
   (-> (execute {:method  :POST
                 :api     "/services/create"
                 :options {:body    service
                           :headers {:Content-Type    "application/json"
                                     :X-Registry-Auth (registry-token auth-config)}}})
       :body)))

(defn update-service
  [id version service]
  (-> (execute {:method  :POST
                :api     (str "/services/" id "/update")
                :options {:body         service
                          :query-params {:version version}
                          :headers      {:Content-Type "application/json"}}})
      :body))

;; Task

(defn tasks
  []
  (-> (execute {:method :GET
                :api    "/tasks"})
      :body))

(defn task
  [id]
  (-> (execute {:method :GET
                :api    (str "/tasks/" id)})
      :body))

;; Network

(defn networks
  []
  (-> (execute {:method :GET
                :api    "/networks"})
      :body))

(defn network
  [id]
  (-> (execute {:method :GET
                :api    (str "/networks/" id)})
      :body))

(defn delete-network
  [id]
  (-> (execute {:method :DELETE
                :api    (str "/networks/" id)})
      :body))

(defn create-network
  [network]
  (-> (execute {:method  :POST
                :api     "/networks/create"
                :options {:body    network
                          :headers {:Content-Type "application/json"}}})
      :body))

;; Volume

(defn volumes
  []
  (-> (execute {:method :GET
                :api    "/volumes"})
      :body))

(defn volume
  [name]
  (-> (execute {:method :GET
                :api    (str "/volumes/" name)})
      :body))

(defn delete-volume
  [name]
  (-> (execute {:method :DELETE
                :api    (str "/volumes/" name)})
      :body))

(defn create-volume
  [volume]
  (-> (execute {:method  :POST
                :api     "/volumes/create"
                :options {:body    volume
                          :headers {:Content-Type "application/json"}}})
      :body))

;; Secret

(defn secrets
  []
  (-> (execute {:method :GET
                :api    "/secrets"})
      :body))

(defn secret
  [id]
  (-> (execute {:method :GET
                :api    (str "/secrets/" id)})
      :body))

(defn delete-secret
  [id]
  (-> (execute {:method :DELETE
                :api    (str "/secrets/" id)})
      :body))

(defn create-secret
  [secret]
  (-> (execute {:method  :POST
                :api     "/secrets/create"
                :options {:body    secret
                          :headers {:Content-Type "application/json"}}})
      :body))

(defn update-secret
  [id version secret]
  (-> (execute {:method  :POST
                :api     (str "/secrets/" id "/update")
                :options {:body         secret
                          :query-params {:version version}
                          :headers      {:Content-Type "application/json"}}})
      :body))

;; Config

(defn configs
  []
  (-> (execute {:method :GET
                :api    "/configs"})
      :body))

(defn config
  [id]
  (-> (execute {:method :GET
                :api    (str "/configs/" id)})
      :body))

(defn delete-config
  [id]
  (-> (execute {:method :DELETE
                :api    (str "/configs/" id)})
      :body))

(defn create-config
  [config]
  (-> (execute {:method  :POST
                :api     "/configs/create"
                :options {:body config}})
      :body))

;; Node

(defn nodes
  []
  (-> (execute {:method :GET
                :api    "/nodes"})
      :body))

(defn node
  [id]
  (-> (execute {:method :GET
                :api    (str "/nodes/" id)})
      :body))

(defn node-tasks
  [id]
  (let [query-params {:filters (generate-string {:node          [id]
                                                 :desired-state ["running"]})}]
    (-> (execute {:method  :GET
                  :api     "/tasks"
                  :options {:query-params query-params}})
        :body)))

(defn version
  []
  (-> (execute {:method :GET
                :api    "/version"})
      :body))

;; Images

(defn image
  [name]
  (-> (execute {:method :GET
                :api    (str "/images/" name "/json")})
      :body))