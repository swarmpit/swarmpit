(ns swarmpit.handler
  (:require [bidi.ring :refer [make-handler]]
            [clojure.walk :as walk]
            [swarmpit.api :as api]))

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

;;; Handler

(def handler
  (make-handler ["/" {"services"  {:get  services
                                   :post service-create}
                      "services/" {:get    {[:id] service}
                                   :delete {[:id] service-delete}}
                      "networks"  {:get networks}
                      "networks/" {:get {[:id] network}}}]))
