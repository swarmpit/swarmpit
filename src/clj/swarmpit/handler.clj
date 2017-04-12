(ns swarmpit.handler
  (:require [bidi.ring :refer (make-handler)]
            [swarmpit.api :as api]))

(defn services
  [_]
  {:status 200 :body (api/services)})

(defn service
  [{:keys [route-params]}]
  {:status 200 :body (api/services (:id route-params))})

(defn service-create
  [_]
  {:status 201 :body "Index"})

(defn service-delete
  [{:keys [route-params]}]
  (api/remove-service (:id route-params))
  {:status 200})

(def handler
  (make-handler ["/" {"services" {:get    {""        services
                                           ["/" :id] service}
                                  :post   service-create
                                  :delete {["/" :id] service-delete}}}]))
