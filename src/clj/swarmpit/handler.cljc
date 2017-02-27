(ns swarmpit.handler
  (:require [bidi.ring :refer (make-handler)]
            [swarmpit.api :as api]))

(defn services-handler [req] {:status 200 :body (api/services)})

(defn create-service-handler [req] {:status 201 :body "Index"})

(def handler
  (make-handler ["/" {"services" {:get  services-handler
                                  :post create-service-handler}}]))
