(ns swarmpit.handler
  (:require [bidi.ring :refer [make-handler]]
            [clojure.walk :as walk]
            [swarmpit.api :as api]))

;;; Service handler

(defn services
  [_]
  {:status 200 :body (api/services)})

(defn service-name
  [_]
  {:status 200 :body (->> (api/services)
                          (map (fn [s] [(:id s) (:serviceName s)]))
                          (into (sorted-map)))})

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

(defn node-name
  [_]
  {:status 200 :body (->> (api/nodes)
                          (map (fn [n] [(:id n) (:name n)]))
                          (into (sorted-map)))})

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

;;; Handler

(def handler
  (make-handler ["/" {"services"  {:get  services
                                   :post service-create}
                      "services/" {:get    {["name"] service-name
                                            [:id]    service}
                                   :delete {[:id] service-delete}
                                   :post   {[:id] service-update}}
                      "networks"  {:get  networks
                                   :post network-create}
                      "networks/" {:get    {[:id] network}
                                   :delete {[:id] network-delete}}
                      "nodes"     {:get nodes}
                      "nodes/"    {:get {["name"] node-name
                                         [:id]    node}}
                      "tasks"     {:get tasks}
                      "tasks/"    {:get {[:id] task}}}]))
