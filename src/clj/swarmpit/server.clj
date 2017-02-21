(ns swarmpit.server
  (:use [org.httpkit.server :only [run-server]])
  (:require [compojure.route :as route]
            [compojure.handler :refer [site]]
            [compojure.core :refer [defroutes GET POST]]))

(defroutes routes
           (GET "/" [] "handling-page")
           (route/not-found "<p>Page not found.</p>"))

(def handler
  (site routes))

(defn -main [& _]
  (run-server handler {:port 8080}))
