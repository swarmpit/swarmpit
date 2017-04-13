(ns swarmpit.handler
  (:require [bidi.ring :refer (make-handler)]
            [swarmpit.api :as api]
            [cheshire.core :refer [parse-string]]))

(defn services
  [_]
  {:status 200 :body (api/services)})

(defn service
  [{:keys [route-params]}]
  {:status 200 :body (api/services (:id route-params))})

(defn service-create
  [{:keys [params]}]
  {:status 201 :body params
           ;(parse-string params)
   ;(keys req)
   })

(defn service-delete
  [{:keys [route-params]}]
  (api/remove-service (:id route-params))
  {:status 200})

(def handler
  (make-handler ["/" {"services" {:get    {""        services
                                           ["/" :id] service}
                                  :post   service-create
                                  :delete {["/" :id] service-delete}}}]))
