(ns swarmpit.server
  (:gen-class)
  (:require [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [swarmpit.authentication :refer [authentication-middleware]]
            [swarmpit.authorization :refer [authorization-middleware]]
            [org.httpkit.server :refer [run-server]]
            [swarmpit.routes :as routes]
            [swarmpit.setup :as setup]
            [swarmpit.database :as db]
            [swarmpit.agent :as agent]
            [reitit.ring :as ring]
            [reitit.coercion.spec]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.dev.pretty :as pretty]
            [reitit.ring.spec :as rrs]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.spec :as rs]
            [expound.alpha :as e]
            [muuntaja.core :as m]
            [taoensso.timbre :refer [info]]))

(defn default-exception-handler
  "Default safe handler for any exception."
  [^Exception e _]
  (let [response (ex-data e)]
    (case (:type response)
      :http-client (dissoc response :headers)
      :aws-client response
      :docker-cli response
      :api response
      {:status 500
       :body   (Throwable->map e)})))

(def app-middleware
  [;; negotiation, request decoding and response encoding
   muuntaja/format-middleware
   ;; authentication
   authentication-middleware
   ;; authorization
   authorization-middleware
   ;; swagger feature
   swagger/swagger-feature
   ;; query-params & form-params
   parameters/parameters-middleware
   ;; exception handling
   (exception/create-exception-middleware
     (merge
       exception/default-handlers
       {:reitit.ring.middleware.exception/default default-exception-handler}))
   ;; coercing response body (disabled)
   ;coercion/coerce-response-middleware
   ;; coercing request parameters
   coercion/coerce-request-middleware])

(def app
  (ring/ring-handler
    (ring/router
      routes/backend
      {:exception   pretty/exception
       :validate    rrs/validate
       ::rs/explain e/expound-str
       :conflicts   nil
       :data        {:coercion   reitit.coercion.spec/coercion
                     :muuntaja   m/instance
                     :middleware app-middleware
                     :swagger    {:produces #{"application/json"
                                              "application/edn"
                                              "application/transit+json"}
                                  :consumes #{"application/json"
                                              "application/edn"
                                              "application/transit+json"}}}})
    (ring/routes
      (swagger-ui/create-swagger-ui-handler
        {:path   "/api-docs"
         :url    "/api/swagger.json"
         :config {:validatorUrl     nil
                  :operationsSorter "alpha"}})
      (-> (ring/create-default-handler)
          (wrap-resource "public")
          (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
          wrap-gzip))))

(defn -main [& [port]]
  (info "Swarmpit is starting...")
  (db/init)
  (let [port (or port 8080)]
    (run-server app {:port port} :thread 8 :queue-size 300000)
    (info "Swarmpit running on port" port))
  (agent/init)
  (setup/docker)
  (setup/log))