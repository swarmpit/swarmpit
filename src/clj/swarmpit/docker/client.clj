(ns swarmpit.docker.client
  (:refer-clojure :exclude [get])
  (:require [ring.util.codec :refer [form-encode]]
            [cheshire.core :refer [generate-string]]
            [swarmpit.base64 :as base64]
            [swarmpit.config :refer [config]]
            [swarmpit.docker.http :refer :all]))

(defn- registry-token
  [auth]
  (base64/encode (generate-string auth)))

;; Service

(defn services
  []
  (get "/services"))

(defn service
  [id]
  (-> (str "/services/" id)
      (get)))

(defn service-tasks
  [id]
  (get "/tasks" {:filters (generate-string {:service [id]})}))

(defn service-logs
  [id opt]
  (-> (str "/services/" id "/logs")
      (get opt)))

(defn delete-service
  [id]
  (-> (str "/services/" id)
      (delete)))

(defn create-service
  ([service]
   (post "/services/create" {} nil service))
  ([auth-config service]
   (let [headers {:X-Registry-Auth (registry-token auth-config)}]
     (post "/services/create" {} headers service))))

(defn update-service
  [id version service]
  (let [uri (str "/services/" id "/update")]
    (post uri {:version version} service)))

;; Task

(defn tasks
  []
  (get "/tasks"))

(defn task
  [id]
  (-> (str "/tasks/" id)
      (get)))

;; Network

(defn networks
  []
  (get "/networks"))

(defn network
  [id]
  (-> (str "/networks/" id)
      (get)))

(defn delete-network
  [id]
  (-> (str "/networks/" id)
      (delete)))

(defn create-network
  [network]
  (post "/networks/create" network))

;; Volume

(defn volumes
  []
  (get "/volumes"))

(defn volume
  [name]
  (-> (str "/volumes/" name)
      (get)))

(defn delete-volume
  [name]
  (-> (str "/volumes/" name)
      (delete)))

(defn create-volume
  [volume]
  (post "/volumes/create" volume))

;; Secret

(defn secrets
  []
  (get "/secrets"))

(defn secret
  [id]
  (-> (str "/secrets/" id)
      (get)))

(defn delete-secret
  [id]
  (-> (str "/secrets/" id)
      (delete)))

(defn create-secret
  [secret]
  (post "/secrets/create" secret))

(defn update-secret
  [id version secret]
  (let [uri (str "/secrets/" id "/update")]
    (post uri {:version version} secret)))

;; Config

(defn configs
  []
  (get "/configs"))

(defn config
  [id]
  (-> (str "/configs/" id)
      (get)))

(defn delete-config
  [id]
  (-> (str "/configs/" id)
      (delete)))

(defn create-config
  [config]
  (post "/configs/create" config))

;; Node

(defn nodes
  []
  (get "/nodes"))

(defn node
  [id]
  (-> (str "/nodes/" id)
      (get)))

(defn node-tasks
  [id]
  (get "/tasks" {:filters (generate-string {:node          [id]
                                            :desired-state ["running"]})}))

(defn version
  []
  (get "/version"))

;; Images

(defn image
  [name]
  (-> (str "/images/" name "/json")
      (get)))