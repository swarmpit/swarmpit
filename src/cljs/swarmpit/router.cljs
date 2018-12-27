(ns swarmpit.router
  (:require [bidi.bidi :as bidi]
            [cemerick.url :refer [query->map]]
            [clojure.walk :refer [keywordize-keys]]
            [pushy.core :as pushy]
            [swarmpit.component.state :as state]
            [swarmpit.routes :as routes]
            [swarmpit.url :refer [dispatch! query-string]]))

(defonce !route (atom {}))

(add-watch !route :watcher
 (fn [key atom old-location new-location]
   (let [query-params (keywordize-keys (query->map (query-string)))
         route-params (:route-params new-location)
         handler (:handler new-location)]
     (state/set-value {:handler handler
                       :params  (merge route-params query-params)} state/route-cursor))))

(defn- set-page! [match]
  (if (= :index (:handler match))
    (reset! !route {:handler :stack-list})
    (reset! !route match)))

(def history
  (pushy/pushy set-page! (partial bidi/match-route routes/frontend)))

(defn set-location
  [location]
  (pushy/set-token! history location))

(defn set-route
  [location]
  (set-page! location))

(defn start
  []
  (pushy/start! history))


