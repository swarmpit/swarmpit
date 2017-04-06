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

(def handler
  (make-handler ["/" {"services" {:get  {""        services
                                         ["/" :id] service}
                                  :post service-create}}]))
