(ns swarmpit.docker.engine.client
  (:require [cheshire.core :refer [generate-string]]
            [clojure.core.memoize :as memo]
            [swarmpit.base64 :as base64]
            [swarmpit.docker.engine.http :refer :all]))

(defn- registry-token
  [auth]
  (base64/encode (generate-string auth)))

(defn- label-query
  [label]
  (when (some? label)
    {:filters (generate-string {:label [label]})}))

;; Data usage

(defn df
  []
  (-> (execute {:method :GET
                :api    "/system/df"})
      :body))

(def df-memo (memo/ttl df :ttl/threshold 10000))

;; Service

(defn services
  ([]
   (-> (execute {:method :GET
                 :api    "/services"})
       :body))
  ([label]
   (-> (execute {:method  :GET
                 :api     "/services"
                 :options {:query-params (label-query label)}})
       :body)))

(defn service
  [id]
  (-> (execute {:method :GET
                :api    (str "/services/" id)})
      :body))

(defn service-tasks
  ([id]
   (service-tasks id false))
  ([id running?]
   (-> (execute {:method  :GET
                 :api     "/tasks"
                 :options {:query-params
                           {:filters
                            (generate-string
                              (merge {:service [id]}
                                     (when running?
                                       {:desired-state ["running"]})))}}})
       :body)))

(defn service-tasks-by-label
  ([label]
   (service-tasks-by-label label false))
  ([label running?]
   (-> (execute {:method  :GET
                 :api     "/tasks"
                 :options {:query-params
                           {:filters
                            (generate-string
                              (merge {:label [label]}
                                     (when running?
                                       {:desired-state ["running"]})))}}})
       :body)))

(defn service-logs
  [id since]
  (-> (execute {:method  :GET
                :api     (str "/services/" id "/logs")
                :options {:query-params {:details    true
                                         :stdout     true
                                         :stderr     true
                                         :timestamps true
                                         :since      since}}})
      :body))

(defn delete-service
  [id]
  (-> (execute {:method :DELETE
                :api    (str "/services/" id)})
      :body))

(defn- x-auth
  [auth-config]
  (when (some? auth-config)
    {:X-Registry-Auth (registry-token auth-config)}))

(defn create-service
  [auth-config service]
  (-> (execute {:method  :POST
                :api     "/services/create"
                :options {:body    service
                          :headers (merge {:Content-Type "application/json"}
                                          (x-auth auth-config))}})
      :body))

(defn update-service
  [auth-config id version service]
  (-> (execute {:method  :POST
                :api     (str "/services/" id "/update")
                :options {:body         service
                          :query-params {:version version}
                          :headers      (merge {:Content-Type "application/json"}
                                               (x-auth auth-config))}})
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
  ([]
   (-> (execute {:method :GET
                 :api    "/networks"})
       :body))
  ([label]
   (-> (execute {:method  :GET
                 :api     "/networks"
                 :options {:query-params (label-query label)}})
       :body)))

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
  ([]
   (-> (execute {:method :GET
                 :api    "/volumes"})
       :body))
  ([label]
   (-> (execute {:method  :GET
                 :api     "/volumes"
                 :options {:query-params (label-query label)}})
       :body)))

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
  ([]
   (-> (execute {:method :GET
                 :api    "/secrets"})
       :body))
  ([label]
   (-> (execute {:method  :GET
                 :api     "/secrets"
                 :options {:query-params (label-query label)}})
       :body)))

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
  ([]
   (-> (execute {:method :GET
                 :api    "/configs"})
       :body))
  ([label]
   (-> (execute {:method  :GET
                 :api     "/configs"
                 :options {:query-params (label-query label)}})
       :body)))

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

(defn update-node
  [id version node]
  (-> (execute {:method  :POST
                :api     (str "/nodes/" id "/update")
                :options {:body         node
                          :query-params {:version version}
                          :headers      {:Content-Type "application/json"}}})
      :body))

(defn delete-node
  [id]
  (-> (execute {:method :DELETE
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

;; System

(defn info
  []
  (-> (execute {:method :GET
                :api    "/info"})
      :body))

(defn version
  []
  (-> (execute {:method :GET
                :api    "/version"})
      :body))

(defn swarm
  []
  (-> (execute {:method :GET
                :api    "/swarm"})
      :body))

;; Images

(defn image
  [name]
  (-> (execute {:method :GET
                :api    (str "/images/" name "/json")})
      :body))

;; Auth

(defn auth
  [distribution]
  (-> (execute {:method  :POST
                :api     "/auth"
                :options {:body {:username      (:username distribution)
                                 :password      (:password distribution)
                                 :serveraddress (or (:url distribution) "https://index.docker.io/v2")}}})
      :body))

(defn exec-create
  []
  (-> (execute {:method  :POST
                :api     "/containers/f58f1834b10e/exec"
                :options {:headers {:Content-Type "application/json"}
                          :body    {:AttachStdin  false
                                    :AttachStdout true
                                    :AttachStderr true
                                    :Tty          false
                                    :Cmd          ["ls"]}}})
      :body))

(defn exec-start
  []
  (-> (execute {:method  :POST
                :api     "/exec/47bce3a1a29fcc70de44eea43b4e71792585f5fb190b1f63a4c8de2670a3d406/start"
                :options {:headers {:Content-Type "application/json"}
                          :body    {:Detach false
                                    :Tty    false}}})
      :body))
