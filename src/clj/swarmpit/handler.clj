(ns swarmpit.handler
  (:require [bidi.ring :refer [make-handler]]
            [clojure.walk :as walk]
            [swarmpit.api :as api]))

;;; Login handler

(defn login
  [{:keys [headers]}]
  {:status 200 :body (api/login (get headers "authorization"))})

;;; Service handler

(defn services
  [_]
  {:status 200 :body (api/services)})

(defn service
  [{:keys [route-params]}]
  {:status 200 :body (api/service (:id route-params))})

(defn service-create
  [{:keys [params]}]
  (let [payload (walk/keywordize-keys params)]
    {:status 201 :body (api/create-service payload)}))

(defn service-update
  [{:keys [route-params params]}]
  (let [payload (walk/keywordize-keys params)]
    (api/update-service (:id route-params) payload)
    {:status 200}))

(defn service-delete
  [{:keys [route-params]}]
  (api/delete-service (:id route-params))
  {:status 200})

;;; Network handler

(defn networks
  [_]
  {:status 200 :body (api/networks)})

(defn network
  [{:keys [route-params]}]
  {:status 200 :body (api/network (:id route-params))})

(defn network-create
  [{:keys [params]}]
  (let [payload (walk/keywordize-keys params)]
    {:status 201 :body (api/create-network payload)}))

(defn network-delete
  [{:keys [route-params]}]
  (api/delete-network (:id route-params))
  {:status 200})

;;; Node handler

(defn nodes
  [_]
  {:status 200 :body (api/nodes)})

(defn node
  [{:keys [route-params]}]
  {:status 200 :body (api/node (:id route-params))})

;;; Task handler

(defn tasks
  [_]
  {:status 200 :body (api/tasks)})

(defn task
  [{:keys [route-params]}]
  {:status 200 :body (api/task (:id route-params))})

;;; Repository handler

(defn repositories
  [_]
  {:status 200 :body (api/registries)})

;;; Registry handler

(defn registry
  [{:keys [route-params]}]
  {:status 200 :body (api/registry (:id route-params))})

(defn registry-create
  [{:keys [params]}]
  (let [payload (walk/keywordize-keys params)]
    {:status 201 :body (api/create-registry payload)}))

;;; Handler

(def handler
  (make-handler ["/" {"login"        {:post login}
                      "services"     {:get  services
                                      :post service-create}
                      "services/"    {:get    {[:id] service}
                                      :delete {[:id] service-delete}
                                      :post   {[:id] service-update}}
                      "networks"     {:get  networks
                                      :post network-create}
                      "networks/"    {:get    {[:id] network}
                                      :delete {[:id] network-delete}}
                      "nodes"        {:get nodes}
                      "nodes/"       {:get {[:id] node}}
                      "tasks"        {:get tasks}
                      "tasks/"       {:get {[:id] task}}
                      "repositories" {:get repositories}
                      "registry"     {:post registry-create}
                      "registry/"    {:get {[:id] registry}}}]))